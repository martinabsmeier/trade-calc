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

import de.am.tcalc.config.PriceProperties;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Live HTTP access to {@code api/v2/stats/history} on the configured server (Europa by default).
 * Tests bind {@code MockRestServiceServer} to the same {@link RestClient.Builder} Spring Boot
 * provides — this class is never pointed at the real API from tests.
 */
@Log4j2
@Component
public class HttpMarketPriceHistoryClient implements MarketPriceHistoryClient {

    private static final ParameterizedTypeReference<List<MarketHistoryItem>> TYPE =
        new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final PriceProperties properties;

    @Autowired
    public HttpMarketPriceHistoryClient(RestClient.Builder builder, PriceProperties properties) {
        this(buildClient(builder, properties), properties);
    }

    /** Test seam: a prebuilt client so {@code MockRestServiceServer}-bound builders stay intact. */
    HttpMarketPriceHistoryClient(RestClient restClient, PriceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    private static RestClient buildClient(RestClient.Builder builder, PriceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeoutMs());
        factory.setReadTimeout(properties.readTimeoutMs());
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    @Override
    public List<MarketHistoryItem> history(List<String> itemIds, String location) {
        if (itemIds == null || itemIds.isEmpty() || !StringUtils.hasText(location)) {
            return List.of();
        }
        // Item ids are bare dump tokens (e.g. T4_2H_BOW) — encode defensively anyway.
        String path = properties.historyPath() + "/" + String.join(",", itemIds);
        String uri = UriComponentsBuilder.fromPath(path)
            .queryParam("locations", location)
            .queryParam("qualities", properties.qualities())
            .queryParam("time-range", properties.historyDays())
            .build()
            .toUriString();
        List<MarketHistoryItem> result = restClient.get().uri(uri).retrieve().body(TYPE);
        return result == null ? List.of() : List.copyOf(result);
    }
}