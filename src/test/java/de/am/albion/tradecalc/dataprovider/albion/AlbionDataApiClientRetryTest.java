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
package de.am.albion.tradecalc.dataprovider.albion;

import de.am.albion.tradecalc.config.AlbionApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the hand-rolled retry behaviour of {@link AlbionDataApiClient}:
 * recovers from a transient {@link ResourceAccessException} on the second
 * attempt, and gives up after {@code MAX_ATTEMPTS} failures.
 */
class AlbionDataApiClientRetryTest {

    private static final String BASE_URL = "https://west.albion-online-data.com";

    private AlbionApiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AlbionApiProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setServer("europe");
        properties.setLocations(java.util.List.of("Lymhurst"));
        properties.setQualities(java.util.List.of(1));
    }

    @Test
    void recoversOnSecondAttempt() {
        FlakyRequestFactory factory = new FlakyRequestFactory(1);
        AlbionDataApiClient client = new AlbionDataApiClient(properties,
                RestClient.builder().requestFactory(factory));

        var result = client.fetchPrices("T4_BOW");

        assertThat(result).isNotEmpty();
        assertThat(factory.callCount).isEqualTo(2);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        FlakyRequestFactory factory = new FlakyRequestFactory(99);
        AlbionDataApiClient client = new AlbionDataApiClient(properties,
                RestClient.builder().requestFactory(factory));

        assertThatThrownBy(() -> client.fetchPrices("T4_BOW"))
                .isInstanceOf(ResourceAccessException.class);
        assertThat(factory.callCount).isEqualTo(AlbionDataApiClient.MAX_ATTEMPTS);
    }

    /**
     * Throws {@link ResourceAccessException} for the first {@code failuresBeforeSuccess}
     * invocations, then returns a canned 200 OK with a single non-empty JSON entry.
     */
    private static final class FlakyRequestFactory implements ClientHttpRequestFactory {
        private static final String BODY =
                "[{\"item_id\":\"T4_BOW\",\"city\":\"Lymhurst\",\"quality\":1,"
                        + "\"sell_price_min\":100,\"sell_price_min_date\":\"2026-01-01T00:00:00Z\","
                        + "\"buy_price_max\":120,\"buy_price_max_date\":\"2026-01-01T00:00:00Z\"}]";

        private final int failuresBeforeSuccess;
        private int callCount;

        FlakyRequestFactory(int failuresBeforeSuccess) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public MockClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri) {
                @Override
                public org.springframework.http.client.ClientHttpResponse executeInternal() throws IOException {
                    callCount++;
                    if (callCount <= failuresBeforeSuccess) {
                        throw new IOException("connection refused");
                    }
                    return new MockClientHttpResponse(BODY.getBytes(), 200) {
                        @Override
                        public HttpHeaders getHeaders() {
                            HttpHeaders h = new HttpHeaders();
                            h.setContentType(MediaType.APPLICATION_JSON);
                            return h;
                        }
                    };
                }
            };
        }
    }
}