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
import de.am.albion.tradecalc.dataprovider.albion.dto.MarketJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AlbionDataApiClient}. A recording
 * {@link ClientHttpRequestFactory} captures the outbound request so the test
 * can assert the URI shape, while a stubbed response drives the JSON
 * deserialisation path. This covers the constructor and most of
 * {@code fetchPrices} without touching real HTTP infrastructure.
 */
class AlbionDataApiClientTest {

    private static final String BASE_URL = "https://west.albion-online-data.com";

    private AlbionApiProperties properties;
    private RecordingRequestFactory factory;
    private AlbionDataApiClient client;

    /**
     * Wires a {@link RecordingRequestFactory} into a fresh
     * {@link RestClient.Builder} so the production code under
     * {@code fetchPrices} is used end-to-end and the outbound request can be
     * inspected.
     */
    @BeforeEach
    void setUp() {
        properties = new AlbionApiProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setServer("europe");
        properties.setLocations(List.of("Lymhurst", "Fort Sterling"));
        properties.setQualities(List.of(1));

        factory = new RecordingRequestFactory();
        client = new AlbionDataApiClient(properties, RestClient.builder().requestFactory(factory));
    }

    /**
     * The single-argument production constructor must succeed with valid
     * properties — no HTTP traffic is initiated at construction time.
     */
    @Test
    void productionConstructor_doesNotPerformHttpCall() {
        AlbionDataApiClient production = new AlbionDataApiClient(properties);

        assertThat(production).isNotNull();
    }

    /**
     * A single-item request produces a URI that includes the {@code items}
     * path segment, the configured {@code locations}, the configured
     * {@code qualities}, and the configured {@code server}.
     */
    @Test
    void fetchPrices_buildsExpectedUri() {
        MarketJson row = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(1500), BigDecimal.valueOf(1450), null, null);
        factory.respondWith(HttpStatus.OK, toJson(new MarketJson[]{row}));

        List<MarketJson> result = client.fetchPrices("T4_BOW");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo("T4_BOW");
        assertThat(factory.capturedUri())
                .asString()
                .startsWith(BASE_URL + "/api/v2/stats/prices/T4_BOW")
                .contains("locations=Lymhurst,Fort%20Sterling")
                .contains("qualities=1")
                .contains("server=europe");
    }

    /**
     * Calling {@code fetchPrices} with no item ids is rejected up front
     * (defensive: would otherwise build a malformed upstream request).
     */
    @Test
    void fetchPrices_noItemIds_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> client.fetchPrices());
    }

    /**
     * A {@code null} varargs argument is also rejected — guards the upstream
     * from a {@link NullPointerException} downstream of the varargs array.
     */
    @Test
    void fetchPrices_nullItemIds_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> client.fetchPrices((String[]) null));
    }

    /**
     * An empty upstream response (HTTP 200 with body {@code null}) is
     * translated to {@link List#of()} so callers can iterate without a
     * {@code null} check.
     */
    @Test
    void fetchPrices_emptyResponse_returnsEmptyList() {
        factory.respondWith(HttpStatus.OK, "");

        List<MarketJson> result = client.fetchPrices("T4_BOW");

        assertThat(result).isEmpty();
    }

    private static String toJson(MarketJson[] rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            MarketJson r = rows[i];
            sb.append('{')
                    .append("\"item_id\":\"").append(r.itemId()).append("\",")
                    .append("\"city\":\"").append(r.city()).append("\",")
                    .append("\"quality\":").append(r.quality()).append(',')
                    .append("\"sell_price_min\":").append(r.sellPriceMin()).append(',')
                    .append("\"buy_price_max\":").append(r.buyPriceMax()).append(',')
                    .append("\"sell_price_min_date\":").append(jsonOrNull(r.sellPriceMinDate())).append(',')
                    .append("\"buy_price_max_date\":").append(jsonOrNull(r.buyPriceMaxDate()))
                    .append('}');
        }
        return sb.append(']').toString();
    }

    private static String jsonOrNull(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    /**
     * Recording factory: stores the URI of the most recent outbound request
     * and serves the preconfigured JSON body as the response. Uses
     * {@link MockClientHttpRequest} so the {@code execute()} contract of
     * {@link ClientHttpRequest} is satisfied.
     */
    private static final class RecordingRequestFactory implements ClientHttpRequestFactory {
        private final AtomicReference<URI> uri = new AtomicReference<>();
        private HttpStatus status = HttpStatus.OK;
        private String body = "";

        void respondWith(HttpStatus status, String body) {
            this.status = status;
            this.body = body;
        }

        URI capturedUri() {
            return uri.get();
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, org.springframework.http.HttpMethod httpMethod) {
            this.uri.set(uri);
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            MockClientHttpResponse response = new MockClientHttpResponse(this.body.getBytes(), this.status);
            response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            request.setResponse(response);
            return request;
        }
    }
}