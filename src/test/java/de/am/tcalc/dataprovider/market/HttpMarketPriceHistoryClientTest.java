/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.tcalc.dataprovider.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.am.tcalc.config.PriceProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpMarketPriceHistoryClientTest {

    // application.yml values (no code-side fallbacks since Boot 4); baseUrl only used via builder.
    private static final PriceProperties TEST_PROPS = new PriceProperties(
        "https://test.local", "/api/v2/stats/history", 28, "1,2,3,4", 1000, 2000);

    // NOTE: never hits the real API — the request below is matched against a stubbed mock server.
    private static final String STUB_BODY = """
        [
          {"location":"Lymhurst","item_id":"T4_2H_BOW","quality":1,"data":[
            {"item_count":5,"avg_price":4331,"timestamp":"2026-10-07T14:00:00"}]},
          {"location":"Lymhurst","item_id":"T4_2H_BOW","quality":2,"data":[]}
        ]
        """;

    @Test
    void buildsUrlWithLocationQualitiesAndRangeAndParsesResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // The MockRestServiceServer-bound builder drops the baseUrl — match the relative URI.
        server.expect(requestTo(
                "/api/v2/stats/history/T4_2H_BOW,T4_2H_BOW@1"
                    + "?locations=Lymhurst&qualities=1,2,3,4&time-range=28"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(STUB_BODY, MediaType.APPLICATION_JSON));

        HttpMarketPriceHistoryClient client = new HttpMarketPriceHistoryClient(
            builder.build(), TEST_PROPS);

        List<MarketHistoryItem> result =
            client.history(List.of("T4_2H_BOW", "T4_2H_BOW@1"), "Lymhurst");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).itemId()).isEqualTo("T4_2H_BOW");
        assertThat(result.get(0).quality()).isEqualTo(1);
        assertThat(result.get(0).data()).hasSize(1);
        assertThat(result.get(0).data().get(0).itemCount()).isEqualTo(5);
        assertThat(result.get(0).data().get(0).avgPrice().intValue()).isEqualTo(4331);
        server.verify();
    }

    @Test
    void emptyBodyYieldsEmptyList() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/api/v2/stats/history/T4_2H_BOW?locations=Lymhurst&qualities=1,2,3,4&time-range=28"))
            .andRespond(withNoContent());
        HttpMarketPriceHistoryClient client = new HttpMarketPriceHistoryClient(
            builder.build(), TEST_PROPS);

        assertThat(client.history(List.of("T4_2H_BOW"), "Lymhurst")).isEmpty();
        server.verify();
    }

    @Test
    void emptyInputReturnsEmptyWithoutHttpCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpMarketPriceHistoryClient client = new HttpMarketPriceHistoryClient(
            builder.build(), TEST_PROPS);

        assertThat(client.history(List.of(), "Lymhurst")).isEmpty();
        assertThat(client.history(null, "Lymhurst")).isEmpty();
        assertThat(client.history(List.of("T4_2H_BOW"), " ")).isEmpty();
        server.verify();
    }
}