package com.sirket.platform.crm.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
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
class DashboardApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PipelineStageRepository stageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User repA;
    private User repB;
    private User manager;
    private PipelineStage firstStage;
    private PipelineStage negotiationStage;

    @BeforeEach
    void seedDashboardData() {
        repA = createUser("dash-rep-a", "ROLE_SALES_REP");
        repB = createUser("dash-rep-b", "ROLE_SALES_REP");
        manager = createUser("dash-manager", "ROLE_SALES_MANAGER");

        firstStage = stageRepository.findByNameIgnoreCase("İlk Temas").orElseThrow();
        negotiationStage = stageRepository.findByNameIgnoreCase("Müzakere").orElseThrow();
    }

    @Test
    void summaryAddsUpOpenWonAndLostFigures() throws Exception {
        createOpportunity(repA, "Açık 1", firstStage, "10000.00");
        createOpportunity(repA, "Açık 2", negotiationStage, "5000.00");
        close(createOpportunity(repA, "Kazanılan", firstStage, "20000.00"), repA, true, null);
        close(createOpportunity(repA, "Kaybedilen", firstStage, "8000.00"), repA, false, "Fiyat");

        mockMvc.perform(get("/api/crm/dashboard/summary").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(2))
                .andExpect(jsonPath("$.openAmount").value(15000.00))
                .andExpect(jsonPath("$.wonCount").value(1))
                .andExpect(jsonPath("$.wonAmount").value(20000.00))
                .andExpect(jsonPath("$.lostCount").value(1))
                .andExpect(jsonPath("$.lostAmount").value(8000.00))
                // 1 won out of 2 closed
                .andExpect(jsonPath("$.winRate").value(50.00));
    }

    @Test
    void winRateIsNullRatherThanZeroWhenNothingHasClosed() throws Exception {
        createOpportunity(repA, "Sadece Açık", firstStage, "1000.00");

        mockMvc.perform(get("/api/crm/dashboard/summary").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.wonCount").value(0))
                .andExpect(jsonPath("$.lostCount").value(0))
                .andExpect(jsonPath("$.winRate").doesNotExist());
    }

    @Test
    void opportunitiesWithoutAnAmountDoNotBreakTheTotals() throws Exception {
        createOpportunity(repA, "Tutarsız", firstStage, null);

        mockMvc.perform(get("/api/crm/dashboard/summary").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.openAmount").value(0));
    }

    @Test
    void summaryCanBeNarrowedToASingleSalesRep() throws Exception {
        createOpportunity(repA, "A'nın Fırsatı", firstStage, "1000.00");
        createOpportunity(repB, "B'nin Fırsatı", firstStage, "2000.00");

        mockMvc.perform(get("/api/crm/dashboard/summary").with(jwtFor(manager)))
                .andExpect(jsonPath("$.openCount").value(2))
                .andExpect(jsonPath("$.openAmount").value(3000.00));

        mockMvc.perform(get("/api/crm/dashboard/summary")
                        .param("owner", repB.getId().toString()).with(jwtFor(manager)))
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.openAmount").value(2000.00));
    }

    @Test
    void pipelineListsEveryStageIncludingEmptyOnes() throws Exception {
        createOpportunity(repA, "İlk Temasta", firstStage, "1000.00");
        createOpportunity(repA, "Müzakerede", negotiationStage, "4000.00");

        mockMvc.perform(get("/api/crm/dashboard/pipeline").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(5000.00))
                // all six seeded stages appear, in display order
                .andExpect(jsonPath("$.stages.length()").value(6))
                .andExpect(jsonPath("$.stages[0].stageName").value("İlk Temas"))
                .andExpect(jsonPath("$.stages[0].count").value(1))
                .andExpect(jsonPath("$.stages[1].stageName").value("Nitelikli"))
                .andExpect(jsonPath("$.stages[1].count").value(0))
                .andExpect(jsonPath("$.stages[1].totalAmount").value(0))
                .andExpect(jsonPath("$.stages[3].stageName").value("Müzakere"))
                .andExpect(jsonPath("$.stages[3].count").value(1))
                .andExpect(jsonPath("$.stages[3].totalAmount").value(4000.00));
    }

    @Test
    void closedOpportunitiesAreExcludedFromThePipelineView() throws Exception {
        close(createOpportunity(repA, "Kapanmış", firstStage, "9000.00"), repA, true, null);

        mockMvc.perform(get("/api/crm/dashboard/pipeline").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void lostReasonsAreGroupedAndRanked() throws Exception {
        close(createOpportunity(repA, "Kayıp 1", firstStage, "1000.00"), repA, false, "Fiyat");
        close(createOpportunity(repA, "Kayıp 2", firstStage, "2000.00"), repA, false, "Fiyat");
        close(createOpportunity(repA, "Kayıp 3", firstStage, "3000.00"), repA, false, "Zamanlama");

        mockMvc.perform(get("/api/crm/reports/lost-reasons").with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLostCount").value(3))
                .andExpect(jsonPath("$.reasons.length()").value(2))
                // most frequent reason first
                .andExpect(jsonPath("$.reasons[0].reason").value("Fiyat"))
                .andExpect(jsonPath("$.reasons[0].count").value(2))
                .andExpect(jsonPath("$.reasons[0].totalAmount").value(3000.00))
                .andExpect(jsonPath("$.reasons[0].share").value(66.67))
                .andExpect(jsonPath("$.reasons[1].reason").value("Zamanlama"))
                .andExpect(jsonPath("$.reasons[1].share").value(33.33));
    }

    @Test
    void expectedCloseDateOutsideTheWindowIsExcluded() throws Exception {
        createOpportunityClosingOn(repA, "Bu Çeyrek", firstStage, "1000.00", LocalDate.of(2026, 3, 15));
        createOpportunityClosingOn(repA, "Gelecek Yıl", firstStage, "9000.00", LocalDate.of(2027, 6, 1));

        mockMvc.perform(get("/api/crm/dashboard/summary")
                        .param("from", "2026-01-01").param("to", "2026-12-31")
                        .with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.openAmount").value(1000.00));
    }

    @Test
    void closedOpportunityCountsOnTheDayItWasClosed() throws Exception {
        close(createOpportunity(repA, "Bugün Kapandı", firstStage, "5000.00"), repA, true, null);
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/crm/dashboard/summary")
                        .param("from", today).param("to", today).with(jwtFor(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wonCount").value(1))
                .andExpect(jsonPath("$.winRate").value(100.00));
    }

    @Test
    void salesRepCannotReachTheDashboard() throws Exception {
        mockMvc.perform(get("/api/crm/dashboard/summary").with(jwtFor(repA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/crm/dashboard/pipeline").with(jwtFor(repA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/crm/reports/lost-reasons").with(jwtFor(repA)))
                .andExpect(status().isForbidden());
    }

    private UUID createOpportunity(User owner, String name, PipelineStage stage, String amount) throws Exception {
        return createOpportunityClosingOn(owner, name, stage, amount, LocalDate.of(2026, 12, 31));
    }

    private UUID createOpportunityClosingOn(User owner, String name, PipelineStage stage, String amount,
            LocalDate expectedCloseDate) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateBody(
                name, null, null, stage.getId(),
                amount == null ? null : new BigDecimal(amount), 50, expectedCloseDate));
        String response = mockMvc.perform(post("/api/crm/opportunities").with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private void close(UUID id, User owner, boolean won, String lostReason) throws Exception {
        String body = objectMapper.writeValueAsString(new CloseBody(won, lostReason));
        mockMvc.perform(patch("/api/crm/opportunities/{id}/close", id).with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private record CreateBody(String name, UUID contactId, UUID companyId, UUID stageId, BigDecimal amount,
            Integer probability, LocalDate expectedCloseDate) {
    }

    private record CloseBody(boolean won, String lostReason) {
    }
}
