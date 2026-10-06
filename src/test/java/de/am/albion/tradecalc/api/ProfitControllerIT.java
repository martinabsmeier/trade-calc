/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.albion.tradecalc.api;

import de.am.albion.tradecalc.api.dto.CalculationResponseDto;
import de.am.albion.tradecalc.domain.CalculationMode;
import de.am.albion.tradecalc.domain.model.CraftingPlan;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.service.PriceServiceAdapter;
import de.am.albion.tradecalc.service.ProfitQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link ProfitController}. Mirrors {@link PriceControllerIT}:
 * Spring boots with a random port, the upstream services are replaced with
 * Mockito stubs so the test stays fast and deterministic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ProfitControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ProfitQueryService queryService;

    @MockBean
    private PriceServiceAdapter prices;

    @Test
    void calculate_returnsRankedEntriesAsJson() {
        ProfitResult profit = ProfitResult.builder()
                .itemId("T4_BOW")
                .city("Lymhurst")
                .totalCost(new BigDecimal("1000.00"))
                .revenue(new BigDecimal("3000.00"))
                .profit(new BigDecimal("2000.00"))
                .profitRatio(new BigDecimal("2.0000"))
                .build();
        ProfitQueryService.ProfitEntry entry =
                new ProfitQueryService.ProfitEntry("T4_BOW", profit, Optional.empty());
        when(queryService.rankTop(eq(CalculationMode.BEST_OF_ALL), anyString(), any(),
                anyInt(), anyBoolean()))
                .thenReturn(List.of(entry));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "BEST_OF_ALL");
        body.put("homeCity", "Lymhurst");
        body.put("top", 20);
        body.put("includePlan", false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<CalculationResponseDto> response = restTemplate.exchange(
                url("/api/v1/profit/calculate"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                CalculationResponseDto.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CalculationResponseDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.mode()).isEqualTo(CalculationMode.BEST_OF_ALL);
        assertThat(dto.homeCity()).isEqualTo("Lymhurst");
        assertThat(dto.includePlan()).isFalse();
        assertThat(dto.returned()).isEqualTo(1);
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).itemId()).isEqualTo("T4_BOW");
        assertThat(dto.items().get(0).profit().profit()).isEqualByComparingTo("2000.00");
        // plan is null because Optional.empty() — Jackson omits NON_NULL fields.
        assertThat(dto.items().get(0).plan()).isNull();
        verify(queryService).rankTop(eq(CalculationMode.BEST_OF_ALL), eq("Lymhurst"),
                any(), eq(20), eq(false));
    }

    @Test
    void calculate_includePlanTrue_attachesPlan() {
        ProfitResult profit = profitOf("T4_BOW");
        CraftingPlan plan = CraftingPlan.builder().build();
        ProfitQueryService.ProfitEntry entry =
                new ProfitQueryService.ProfitEntry("T4_BOW", profit, Optional.of(plan));
        when(queryService.rankTop(any(), anyString(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(entry));

        Map<String, Object> body = Map.of(
                "mode", "LOCAL_ONLY",
                "homeCity", "Lymhurst",
                "top", 0,
                "includePlan", true);

        ResponseEntity<CalculationResponseDto> response = restTemplate.exchange(
                url("/api/v1/profit/calculate"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                CalculationResponseDto.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().items().get(0).plan()).isNotNull();
    }

    @Test
    void calculate_missingMode_returnsBadRequest() {
        Map<String, Object> body = Map.of("homeCity", "Lymhurst", "top", 20, "includePlan", false);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/profit/calculate"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("validation_failed");
    }

    @Test
    void calculate_blankHomeCity_returnsBadRequest() {
        Map<String, Object> body = Map.of(
                "mode", "LOCAL_ONLY",
                "homeCity", "",
                "top", 20,
                "includePlan", false);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/profit/calculate"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("homeCity");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static ProfitResult profitOf(String itemId) {
        return ProfitResult.builder()
                .itemId(itemId).city("Lymhurst")
                .totalCost(BigDecimal.ZERO).revenue(BigDecimal.ZERO)
                .profit(BigDecimal.ZERO).profitRatio(BigDecimal.ZERO)
                .build();
    }
}