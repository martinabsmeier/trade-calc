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

import de.am.albion.tradecalc.domain.model.MarketPrice;
import de.am.albion.tradecalc.service.PriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link PriceController}. The {@link PriceService}
 * bean is replaced with a Mockito mock so the test stays fast and
 * deterministic — the live AlbionData API is <strong>not</strong> called here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class PriceControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private PriceService priceService;

    /**
     * The endpoint returns the {@code Map<String, MarketPrice>} produced by
     * the service as JSON, preserving the city keys and the numeric fields.
     */
    @Test
    void getPrices_returnsServiceResultAsJson() {
        MarketPrice lym = MarketPrice.builder()
                .itemId("T4_BOW").city("Lymhurst").quality(1)
                .buyPriceMax(new BigDecimal("1500"))
                .sellPriceMin(new BigDecimal("1450"))
                .observedAt(Instant.parse("2026-10-05T05:00:00Z"))
                .build();
        MarketPrice fort = MarketPrice.builder()
                .itemId("T4_BOW").city("Fort Sterling").quality(1)
                .buyPriceMax(new BigDecimal("1600"))
                .sellPriceMin(new BigDecimal("1550"))
                .observedAt(Instant.parse("2026-10-05T05:00:00Z"))
                .build();
        when(priceService.getPrices("T4_BOW")).thenReturn(Map.of(
                "Lymhurst", lym,
                "Fort Sterling", fort));

        ResponseEntity<Map<String, MarketPrice>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/prices/T4_BOW",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, MarketPrice>>() {});

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, MarketPrice> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsOnlyKeys("Lymhurst", "Fort Sterling");
        assertThat(body.get("Lymhurst").sellPriceMin()).isEqualByComparingTo("1450");
        assertThat(body.get("Fort Sterling").buyPriceMax()).isEqualByComparingTo("1600");
    }

    /**
     * An empty service result still yields HTTP&nbsp;200 — the controller must
     * not 404 just because the item is unknown.
     */
    @Test
    void getPrices_unknownItem_returnsEmptyMap() {
        when(priceService.getPrices("T9_UNKNOWN")).thenReturn(Map.of());

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/prices/T9_UNKNOWN", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }
}