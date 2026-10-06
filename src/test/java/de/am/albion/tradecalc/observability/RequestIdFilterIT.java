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
package de.am.albion.tradecalc.observability;

import de.am.albion.tradecalc.service.PriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip test for {@link RequestIdFilter}: incoming {@code X-Request-Id}
 * is honoured and echoed in the response, an absent header is replaced with a
 * fresh id.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class RequestIdFilterIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private PriceService priceService;

    @Test
    void incomingRequestIdIsEchoed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", "trace-12345");

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/prices/T4_BOW",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("trace-12345");
    }

    @Test
    void missingRequestIdIsMintedAsUuid() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/prices/T4_BOW", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String minted = response.getHeaders().getFirst("X-Request-Id");
        assertThat(minted).isNotNull();
        assertThat(minted).matches("[0-9a-fA-F-]{36}"); // looks like a UUID
    }
}