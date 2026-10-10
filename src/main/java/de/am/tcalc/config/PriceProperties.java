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
package de.am.tcalc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the albion-online-data.com market history fetch (spec: data source, Europa server).
 * All defaults live in {@code application.yml} — the record carries no built-in fallbacks, so a
 * missing key surfaces at startup/testing instead of being silently substituted in code.
 *
 * @param baseUrl API host, e.g. the Europa server
 * @param historyPath path of the history endpoint
 * @param historyDays history window in days (the spec's "last 4 weeks")
 * @param qualities quality levels queried in one request
 * @param connectTimeoutMs connect timeout
 * @param readTimeoutMs read timeout
 * @param maxRetries extra attempts on 429 or connection failure before giving up
 * @param retryMaxWaitMs upper bound (ms) for the wait between retries, e.g. a clamped Retry-After
 */
@ConfigurationProperties(prefix = "price")
public record PriceProperties(
    String baseUrl,
    String historyPath,
    int historyDays,
    String qualities,
    int connectTimeoutMs,
    int readTimeoutMs,
    int maxRetries,
    int retryMaxWaitMs
) {
}