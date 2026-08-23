package com.sirket.platform.crm.contact;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.Role;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.repository.RoleRepository;
import com.sirket.platform.common.identity.repository.UserRepository;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.CompanyRepository;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ContactApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User repA;
    private User repB;
    private User manager;
    private Company acme;

    @BeforeEach
    void seedCrmData() {
        repA = createUser("rep-a", "ROLE_SALES_REP");
        repB = createUser("rep-b", "ROLE_SALES_REP");
        manager = createUser("manager", "ROLE_SALES_MANAGER");

        acme = companyRepository.save(new Company("Acme A.Ş.", "Yazılım", "acme.test", "İstanbul", repA.getId()));
        contactRepository.save(new Contact("Ayşe", "Yılmaz", "ayse@acme.test", "+905551112233",
                "Satın Alma Müdürü", acme, "FUAR", repA.getId()));
        contactRepository.save(new Contact("Mehmet", "Demir", "mehmet@beta.test", "+905554445566",
                "CTO", null, "WEB", repB.getId()));
    }

    @Test
    void salesRepSeesOnlyItsOwnContacts() throws Exception {
        mockMvc.perform(get("/api/crm/contacts").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ayşe"))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme A.Ş."));
    }

    @Test
    void salesManagerSeesEveryContact() throws Exception {
        mockMvc.perform(get("/api/crm/contacts").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void salesRepCannotReadAnotherRepsContact() throws Exception {
        UUID otherContactId = contactRepository.findAll().stream()
                .filter(c -> c.getOwnerUserId().equals(repB.getId()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/crm/contacts/{id}", otherContactId).with(jwtFor(repA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createdContactIsOwnedByItsCreator() throws Exception {
        String body = objectMapper.writeValueAsString(new ContactBody(
                "Zeynep", "Kaya", "zeynep@gamma.test", "+905557778899", "Genel Müdür", null, "REFERANS"));

        mockMvc.perform(post("/api/crm/contacts").with(jwtFor(repB))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUserId").value(repB.getId().toString()));
    }

    @Test
    void duplicateCheckReportsMatchWithoutBlocking() throws Exception {
        mockMvc.perform(get("/api/crm/contacts/check-duplicate")
                        .param("email", "AYSE@ACME.TEST")
                        .with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicateFound").value(true))
                .andExpect(jsonPath("$.matches[0].firstName").value("Ayşe"));

        // The duplicate is only a warning: creating the same e-mail still succeeds.
        String body = objectMapper.writeValueAsString(new ContactBody(
                "Ayşe", "Yılmaz", "ayse@acme.test", null, null, null, "FUAR"));
        mockMvc.perform(post("/api/crm/contacts").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateCheckWithoutAnyParameterIsRejected() throws Exception {
        mockMvc.perform(get("/api/crm/contacts/check-duplicate").with(jwtFor(repA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void salesRepMayNotDeleteContacts() throws Exception {
        UUID ownContactId = contactRepository.findAll().stream()
                .filter(c -> c.getOwnerUserId().equals(repA.getId()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(delete("/api/crm/contacts/{id}", ownContactId).with(jwtFor(repA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteBySalesManagerHidesTheContactButKeepsTheRow() throws Exception {
        UUID contactId = contactRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/crm/contacts/{id}", contactId).with(jwtFor(manager)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/crm/contacts/{id}", contactId).with(jwtFor(manager)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nameFilterMatchesAcrossFirstAndLastName() throws Exception {
        mockMvc.perform(get("/api/crm/contacts").param("name", "yılmaz").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Yılmaz"));
    }

    @Test
    void companyListIsScopedToTheOwningSalesRep() throws Exception {
        companyRepository.save(new Company("Beta Ltd.", "Lojistik", null, "İzmir", repB.getId()));

        mockMvc.perform(get("/api/crm/companies").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Acme A.Ş."));

        mockMvc.perform(get("/api/crm/companies").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void companyIndustryFilterIsCaseInsensitive() throws Exception {
        companyRepository.save(new Company("Beta Ltd.", "Lojistik", null, "İzmir", repB.getId()));

        mockMvc.perform(get("/api/crm/companies").param("industry", "lojistik").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Beta Ltd."));
    }

    @Test
    void companyDeletionIsRejectedWhileContactsStillReferenceIt() throws Exception {
        mockMvc.perform(delete("/api/crm/companies/{id}", acme.getId()).with(jwtFor(manager)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void companyUpdateByItsOwnerSucceeds() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CompanyBody("Acme Teknoloji A.Ş.", "Yazılım", "acme.test", "Ankara"));

        mockMvc.perform(put("/api/crm/companies/{id}", acme.getId()).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Teknoloji A.Ş."))
                .andExpect(jsonPath("$.address").value("Ankara"));
    }

    @Test
    void companyUpdateByAnotherRepIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(new CompanyBody("Ele Geçirildi", null, null, null));

        mockMvc.perform(put("/api/crm/companies/{id}", acme.getId()).with(jwtFor(repB))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidEmailIsRejectedByValidation() throws Exception {
        String body = objectMapper.writeValueAsString(new ContactBody(
                "Ali", "Veli", "bu-bir-eposta-degil", null, null, null, null));

        mockMvc.perform(post("/api/crm/contacts").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    private User createUser(String username, String role) {
        Role assigned = roleRepository.findByName(role).orElseThrow();
        User user = new User(username, username + "@example.com", passwordEncoder.encode("test-password-1234"));
        user.replaceRoles(Set.of(assigned));
        return userRepository.save(user);
    }

    /**
     * Builds the same JWT shape the auth endpoint issues: subject is the user id and roles live
     * in a {@code roles} claim.
     */
    private RequestPostProcessor jwtFor(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
        return jwt()
                .jwt(token -> token.subject(user.getId().toString()).claim("roles", roles))
                .authorities(authorities);
    }

    private record ContactBody(String firstName, String lastName, String email, String phone, String title,
            UUID companyId, String source) {
    }

    private record CompanyBody(String name, String industry, String website, String address) {
    }
}
