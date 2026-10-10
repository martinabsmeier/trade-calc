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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Live HTTP access to {@code api/v2/stats/history} on the configured server (Europa by default).
 * Tests bind {@code MockRestServiceServer} to a {@link RestClient.Builder} and construct the
 * client via the package-private prebuilt-{@link RestClient} constructor — this class is never
 * pointed at the real API from tests.
 */
@Log4j2
@Component
public class HttpMarketPriceHistoryClient implements MarketPriceHistoryClient {

    /** Wait between retries when the server sends no (parseable) Retry-After. */
    static final long DEFAULT_RETRY_WAIT_MS = 500;

    private static final ParameterizedTypeReference<List<MarketHistoryItem>> TYPE =
        new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final PriceProperties properties;

    /** DI constructor — required because the test-seam constructor would otherwise be ambiguous. */
    @Autowired
    public HttpMarketPriceHistoryClient(PriceProperties properties) {
        this(buildClient(RestClient.builder(), properties), properties);
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
        // The throttled AODP API demands compression ("be nice … use compression"). The JDK
        // HttpURLConnection behind SimpleClientHttpRequestFactory neither negotiates gzip nor
        // decodes it, so the interceptor below must do the decoding.
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory)
            .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
            .requestInterceptor(new GzipDecoder())
            .build();
    }

    /** Unpacks gzip responses; identity pass-through for anything the server sends uncompressed. */
    @Log4j2
    static class GzipDecoder implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            ClientHttpResponse response = execution.execute(request, body);
            if (!"gzip".equalsIgnoreCase(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
                return response;
            }
            log.debug("Unpacking gzipped response from {}", request.getURI());
            byte[] gzipped = response.getBody().readAllBytes();
            response.close();
            return new UnpackedResponse(response, new GZIPInputStream(new ByteArrayInputStream(gzipped)));
        }
    }

    /** Wraps the server response so only {@link #getBody} differs — gzip already unpacked. */
    private record UnpackedResponse(ClientHttpResponse delegate, InputStream body)
            implements ClientHttpResponse {

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public void close() {
            try (InputStream in = body) {
                // Closing the unpacking stream releases the gzip buffers.
            } catch (IOException e) {
                log.debug("Ignoring error while closing gzipped response", e);
            }
            delegate.close();
        }
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

        // The AODP API throttles bursts (429 with a Retry-After) and sometimes drops the
        // connection mid-request ("Error writing to server") — both are transient, so both
        // get the same bounded retry budget (Spring's @Cacheable never caches the throwing
        // attempt — if all retries fail, the exception surfaces to the caller).
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            try {
                return fetch(uri);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == properties.maxRetries()) {
                    log.warn("Market history throttled after {} attempt(s): {}",
                        attempt + 1, e.getResponseBodyAsString());
                    throw e;
                }
                log.info("Market history throttled, retrying");
                waitForRetry(whenToRetry(e.getResponseHeaders()));
            } catch (ResourceAccessException e) {
                if (attempt == properties.maxRetries()) {
                    log.warn("Market history request failed after {} attempt(s)", attempt + 1, e);
                    throw e;
                }
                log.info("Market history connection failed, retrying", e);
                waitForRetry(DEFAULT_RETRY_WAIT_MS);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private List<MarketHistoryItem> fetch(String uri) {
        List<MarketHistoryItem> result = restClient.get().uri(uri).retrieve().body(TYPE);
        return result == null ? List.of() : List.copyOf(result);
    }

    /** Wait time derived from the server's Retry-After (seconds or HTTP-date), clamped. */
    long whenToRetry(HttpHeaders headersFromResponse) {
        if (headersFromResponse == null) {
            return DEFAULT_RETRY_WAIT_MS;
        }
        String raw = headersFromResponse.getFirst(HttpHeaders.RETRY_AFTER);
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_RETRY_WAIT_MS;
        }
        try {
            return clamp(Long.parseLong(raw.trim()) * 1000L);
        } catch (NumberFormatException seconds) {
            try {
                return clamp(Duration.ofMillis(
                    ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().minusMillis(System.currentTimeMillis()).toEpochMilli())
                    .toMillis());
            } catch (DateTimeParseException date) {
                log.info("Unparseable Retry-After header: {}", raw);
                return DEFAULT_RETRY_WAIT_MS;
            }
        }
    }

    private long clamp(long waitMs) {
        return Math.max(0, Math.min(waitMs, properties.retryMaxWaitMs()));
    }

    private void waitForRetry(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", e);
        }
    }
}