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
package de.am.albion.tradecalc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the <strong>Spring Boot Actuator</strong> endpoints exposed
 * by the application. The tests start a full Spring context with an embedded servlet
 * container on a random port and call the Actuator HTTP endpoints over HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ActuatorEndpointsIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Verifies that the {@code /actuator/health} endpoint returns HTTP&nbsp;200
     * and reports overall application status {@code UP}.
     */
    @Test
    void healthEndpoint_returnsUp() {
        ResponseEntity<Map> response = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/health", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("status", "UP");
    }

    /**
     * Verifies that the {@code /actuator/caches} endpoint is reachable and that
     * the configured {@link CacheManager} exposes all caches the application
     * relies on (market prices, recipes, city bonuses).
     */
    @Test
    void cachesEndpoint_listsAllConfiguredCaches() {
        ResponseEntity<Map> response = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/caches", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(cacheManager.getCacheNames())
                .contains("marketPrices", "recipes", "cityBonuses");
    }
}