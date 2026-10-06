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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin HTTP client for the <strong>Albion Online Data API</strong>
 * (<code>albion-online-data.com</code>). Wraps {@link RestClient} and returns
 * raw {@link MarketJson} records — mapping to domain objects happens in
 * {@link de.am.albion.tradecalc.dataprovider.albion.mapper.MarketJsonMapper}.
 *
 * <p>One request fetches the full price matrix for the given items at all
 * configured locations and qualities (per {@link AlbionApiProperties}).</p>
 *
 * <p>Endpoint: {@code GET /api/v2/stats/prices/{items}?locations=…&qualities=…&server=…}.
 * v2 is used instead of v1 because v1 is no longer served on the
 * {@code west} shard.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Component
public class AlbionDataApiClient {

    private final RestClient restClient;
    private final AlbionApiProperties properties;

    /**
     * Total attempts on transient failures: one initial call plus two retries.
     * ponytail: a hand-rolled retry keeps the dependency surface clean. Upgrade
     * path is Resilience4j once we need circuit breaking or rate limiting.
     */
    static final int MAX_ATTEMPTS = 3;
    /** Initial backoff in milliseconds; doubled after every retry attempt. */
    static final long RETRY_BACKOFF_MS = 200L;

    /**
     * Creates a client configured against {@link AlbionApiProperties#getBaseUrl()}.
     *
     * @param properties bound API configuration (base URL, locations, qualities, server)
     */
    @Autowired
    public AlbionDataApiClient(AlbionApiProperties properties) {
        this(properties, RestClient.builder());
    }

    /**
     * Creates a client with a caller-supplied {@link RestClient.Builder}.
     * Test seam — production code uses the single-arg constructor.
     *
     * @param properties bound API configuration
     * @param builder    configured RestClient builder; the caller retains ownership
     */
    AlbionDataApiClient(AlbionApiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * Fetches the price matrix for the given item identifiers. Retries on
     * transient {@link org.springframework.web.client.ResourceAccessException}
     * (connection refused, read timeout, …) up to {@link #MAX_ATTEMPTS} times
     * with exponential backoff; non-transient failures bubble up immediately.
     *
     * @param itemIds one or more Albion item identifiers (e.g. {@code "T4_BOW"});
     *                must not be empty
     * @return raw upstream response, one per {@code (item, city, quality)} triple;
     *         empty list if the upstream returns an empty array
     */
    public List<MarketJson> fetchPrices(String... itemIds) {
        if (itemIds == null || itemIds.length == 0) {
            throw new IllegalArgumentException("At least one item id is required");
        }
        long backoff = RETRY_BACKOFF_MS;
        org.springframework.web.client.ResourceAccessException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return doFetch(itemsParam(itemIds));
            } catch (org.springframework.web.client.ResourceAccessException ex) {
                lastFailure = ex;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("Albion API call failed (attempt {}/{}): {} — retrying in {} ms",
                        attempt, MAX_ATTEMPTS, ex.getMessage(), backoff);
                sleep(backoff);
                backoff *= 2;
            }
        }
        throw lastFailure;
    }

    private List<MarketJson> doFetch(String items) {
        MarketJson[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/stats/prices/{items}")
                        .queryParam("locations", String.join(",", properties.getLocations()))
                        .queryParam("qualities", properties.getQualities().stream()
                                .map(String::valueOf).collect(Collectors.joining(",")))
                        .queryParam("server", properties.getServer())
                        .build(items))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MarketJson[].class);

        List<MarketJson> body = response == null ? List.of() : Arrays.asList(response);
        log.debug("Fetched {} price records for {} item(s)", body.size(), items);
        return body;
    }

    private static String itemsParam(String[] itemIds) {
        return String.join(",", itemIds);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry backoff", e);
        }
    }
}