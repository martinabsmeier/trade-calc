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
package de.am.albion.tradecalc.ui;

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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.List;
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
 * Round-trip tests for the Thymeleaf UI. Spring boots with a random port
 * and the upstream services are mocked so the tests stay deterministic — no
 * Albion API is hit and no live prices are involved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DashboardControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ProfitQueryService queryService;

    @MockBean
    private PriceServiceAdapter prices;

    @Test
    void dashboard_rendersFormWithCitiesAndModes() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String body = response.getBody();
        assertThat(body).contains("Crafting profit calculator");
        assertThat(body).contains("Lymhurst");
        assertThat(body).contains("Fort Sterling");
        assertThat(body).contains("BEST_OF_ALL");
    }

    @Test
    void submit_redirectsToResultsAndCarriesQueryString() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("homeCity", "Lymhurst");
        form.add("mode", "LOCAL_ONLY");
        form.add("top", "5");
        form.add("includePlan", "false");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/"),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class);

        // Post/Redirect/Get: 302 with Location pointing at /results…
        assertThat(response.getStatusCode().value()).isIn(302, 303);
        String location = response.getHeaders().getFirst("Location");
        assertThat(location).isNotNull();
        assertThat(location).contains("/results");
        assertThat(location).contains("mode=LOCAL_ONLY");
        assertThat(location).contains("homeCity=Lymhurst");
        assertThat(location).contains("top=5");
    }

    @Test
    void results_rendersRankedItemsInTable() {
        ProfitResult profit = profitOf("T4_BOW");
        when(queryService.rankTop(any(), anyString(), any(), anyInt(), anyBoolean()))
                .thenReturn(List.of(new ProfitQueryService.ProfitEntry("T4_BOW", profit, Optional.empty())));

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/results?mode=BEST_OF_ALL&homeCity=Lymhurst&top=20&includePlan=false"),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String body = response.getBody();
        assertThat(body).contains("T4_BOW");
        assertThat(body).contains("Top items");
        verify(queryService).rankTop(eq(CalculationMode.BEST_OF_ALL), eq("Lymhurst"),
                any(), eq(20), eq(false));
    }

    @Test
    void planFragment_returnsDrawerForKnownItem() {
        CraftingPlan plan = CraftingPlan.builder()
                .profitSummary(profitOf("T4_BOW"))
                .steps(List.of())
                .build();
        when(queryService.planFor(eq("T4_BOW"), any(), anyString(), any()))
                .thenReturn(Optional.of(plan));

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/plan?itemId=T4_BOW&mode=BEST_OF_ALL&homeCity=Lymhurst"),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String body = response.getBody();
        assertThat(body).contains("Plan for");
        assertThat(body).contains("T4_BOW");
        assertThat(body).doesNotContain("No crafting plan available");
        verify(queryService).planFor(eq("T4_BOW"), eq(CalculationMode.BEST_OF_ALL),
                eq("Lymhurst"), any());
    }

    @Test
    void planFragment_missingItem_returnsWarning() {
        when(queryService.planFor(eq("T9_MISSING"), any(), anyString(), any()))
                .thenReturn(Optional.empty());

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/plan?itemId=T9_MISSING&mode=BEST_OF_ALL&homeCity=Lymhurst"),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("No crafting plan available");
        assertThat(response.getBody()).contains("T9_MISSING");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static ProfitResult profitOf(String itemId) {
        return ProfitResult.builder()
                .itemId(itemId).city("Lymhurst")
                .totalCost(new BigDecimal("1000.00"))
                .revenue(new BigDecimal("3000.00"))
                .profit(new BigDecimal("2000.00"))
                .profitRatio(new BigDecimal("2.0000"))
                .build();
    }
}