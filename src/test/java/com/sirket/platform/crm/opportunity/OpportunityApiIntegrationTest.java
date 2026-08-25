package com.sirket.platform.crm.opportunity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.CompanyRepository;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import com.sirket.platform.crm.opportunity.domain.PipelineStage;
import com.sirket.platform.crm.opportunity.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OpportunityApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private PipelineStageRepository stageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User repA;
    private User repB;
    private User manager;
    private User admin;
    private Company acme;
    private Contact contactOfRepA;
    private Contact contactOfRepB;
    private PipelineStage firstStage;
    private PipelineStage negotiationStage;
    private PipelineStage wonStage;
    private PipelineStage lostStage;

    @BeforeEach
    void seedOpportunityData() {
        repA = createUser("opp-rep-a", "ROLE_SALES_REP");
        repB = createUser("opp-rep-b", "ROLE_SALES_REP");
        manager = createUser("opp-manager", "ROLE_SALES_MANAGER");
        admin = createUser("opp-admin", "ROLE_ADMIN");

        acme = companyRepository.save(
                new Company("Acme A.Ş.", "Yazılım", "acme.test", "İstanbul", repA.getId()));
        contactOfRepA = contactRepository.save(new Contact("Ayşe", "Yılmaz", "ayse@acme.test",
                "+905551112233", "Satın Alma", acme, "FUAR", repA.getId()));
        contactOfRepB = contactRepository.save(new Contact("Mehmet", "Demir", "mehmet@beta.test",
                "+905554445566", "CTO", null, "WEB", repB.getId()));

        firstStage = stageRepository.findByNameIgnoreCase("İlk Temas").orElseThrow();
        negotiationStage = stageRepository.findByNameIgnoreCase("Müzakere").orElseThrow();
        wonStage = stageRepository.findFirstByWonStageIsTrue().orElseThrow();
        lostStage = stageRepository.findFirstByLostStageIsTrue().orElseThrow();
    }

    @Test
    void seededPipelineStagesAreReturnedInOrder() throws Exception {
        mockMvc.perform(get("/api/crm/pipeline-stages").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("İlk Temas"))
                .andExpect(jsonPath("$[4].name").value("Kazanıldı"))
                .andExpect(jsonPath("$[4].wonStage").value(true))
                .andExpect(jsonPath("$[5].lostStage").value(true));
    }

    @Test
    void salesRepMayNotDefinePipelineStages() throws Exception {
        String body = objectMapper.writeValueAsString(new StageBody("Ön Değerlendirme", 7, false, false));

        mockMvc.perform(post("/api/crm/pipeline-stages").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void stageCannotBeBothWonAndLost() throws Exception {
        String body = objectMapper.writeValueAsString(new StageBody("Tuhaf Aşama", 9, true, true));

        mockMvc.perform(post("/api/crm/pipeline-stages").with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void createdOpportunityStartsOpenAndBelongsToItsCreator() throws Exception {
        mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody(
                                "ERP Projesi", contactOfRepA.getId(), acme.getId(), firstStage.getId(),
                                new BigDecimal("150000.00"), 30, LocalDate.of(2026, 12, 31)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.stageName").value("İlk Temas"))
                .andExpect(jsonPath("$.amount").value(150000.00))
                .andExpect(jsonPath("$.contactName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.companyName").value("Acme A.Ş."))
                .andExpect(jsonPath("$.ownerUserId").value(repA.getId().toString()));
    }

    @Test
    void opportunityCannotBeOpenedDirectlyOnAClosingStage() throws Exception {
        mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Hatalı Fırsat", null, wonStage.getId(), null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void salesRepCannotAttachAnotherRepsContact() throws Exception {
        mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Başkasının Kişisi", contactOfRepB.getId(), firstStage.getId(), null, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void opportunityMovesBetweenOpenStages() throws Exception {
        UUID id = createOpportunity(repA, "Müzakereye Giden", contactOfRepA.getId(), firstStage.getId());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/stage", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + negotiationStage.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Müzakere"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    /**
     * The heart of FR-CRM-09: if a stage move could land on "Kaybedildi" the mandatory loss reason
     * would be bypassable, so that move must be refused.
     */
    @Test
    void stageMoveOntoALostStageIsRefusedSoTheLossReasonCannotBeSkipped() throws Exception {
        UUID id = createOpportunity(repA, "Kaçak Kapanış", null, firstStage.getId());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/stage", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + lostStage.getId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/crm/opportunities/{id}", id).with(jwtFor(repA)))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void closingAsLostRequiresAReason() throws Exception {
        UUID id = createOpportunity(repA, "Nedensiz Kayıp", null, firstStage.getId());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"won\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void closingAsLostStoresTheReasonAndMovesToTheLostStage() throws Exception {
        UUID id = createOpportunity(repA, "Fiyat Kaybı", null, firstStage.getId());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"won\":false,\"lostReason\":\"Fiyat rakipten yüksek\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.lostReason").value("Fiyat rakipten yüksek"))
                .andExpect(jsonPath("$.stageName").value("Kaybedildi"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void closingAsWonClearsAnyLossReasonAndMovesToTheWonStage() throws Exception {
        UUID id = createOpportunity(repA, "Kazanılan İş", null, firstStage.getId());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"won\":true,\"lostReason\":\"gonderilse bile yok sayilmali\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.stageName").value("Kazanıldı"))
                .andExpect(jsonPath("$.lostReason").doesNotExist());
    }

    @Test
    void anAlreadyClosedOpportunityCannotBeClosedOrMovedAgain() throws Exception {
        UUID id = createOpportunity(repA, "Tek Sefer Kapanır", null, firstStage.getId());
        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"won\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"won\":true}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/crm/opportunities/{id}/stage", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + negotiationStage.getId() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void salesRepSeesOnlyItsOwnOpportunitiesWhileManagerSeesAll() throws Exception {
        createOpportunity(repA, "A Fırsatı", contactOfRepA.getId(), firstStage.getId());
        createOpportunity(repB, "B Fırsatı", contactOfRepB.getId(), firstStage.getId());

        mockMvc.perform(get("/api/crm/opportunities").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("A Fırsatı"));

        mockMvc.perform(get("/api/crm/opportunities").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listCanBeFilteredByStatus() throws Exception {
        UUID won = createOpportunity(repA, "Kazanılacak", null, firstStage.getId());
        createOpportunity(repA, "Açık Kalacak", null, firstStage.getId());
        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", won).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"won\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/crm/opportunities").param("status", "WON").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Kazanılacak"));

        mockMvc.perform(get("/api/crm/opportunities").param("status", "OPEN").with(jwtFor(repA)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Açık Kalacak"));
    }

    @Test
    void opportunitiesOfAContactAreListedOnItsOwnEndpoint() throws Exception {
        createOpportunity(repA, "Kişiye Bağlı", contactOfRepA.getId(), firstStage.getId());
        createOpportunity(repA, "Bağımsız", null, firstStage.getId());

        mockMvc.perform(get("/api/crm/contacts/{id}/opportunities", contactOfRepA.getId()).with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Kişiye Bağlı"));
    }

    @Test
    void salesRepMayNotDeleteOpportunities() throws Exception {
        UUID id = createOpportunity(repA, "Silinemez", null, firstStage.getId());

        mockMvc.perform(delete("/api/crm/opportunities/{id}", id).with(jwtFor(repA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/crm/opportunities/{id}", id).with(jwtFor(manager)))
                .andExpect(status().isNoContent());
    }

    @Test
    void probabilityOutsideZeroToHundredIsRejected() throws Exception {
        mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Olasılık Hatası", null, firstStage.getId(), null, 150)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("probability"));
    }

    @Test
    void negativeAmountIsRejected() throws Exception {
        mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Negatif Tutar", null, firstStage.getId(), new BigDecimal("-5"), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    private UUID createOpportunity(User owner, String name, UUID contactId, UUID stageId) throws Exception {
        String response = mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(name, contactId, stageId, new BigDecimal("1000.00"), 50)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String createBody(String name, UUID contactId, UUID stageId, BigDecimal amount, Integer probability) {
        return objectMapper.writeValueAsString(new CreateBody(
                name, contactId, null, stageId, amount, probability, LocalDate.of(2026, 12, 31)));
    }

    private record CreateBody(String name, UUID contactId, UUID companyId, UUID stageId, BigDecimal amount,
            Integer probability, LocalDate expectedCloseDate) {
    }

    private record StageBody(String name, int displayOrder, boolean wonStage, boolean lostStage) {
    }
}
