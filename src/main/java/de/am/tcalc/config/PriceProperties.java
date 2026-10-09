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
 *
 * @param baseUrl API host; defaults to the Europa server when unset or blank
 * @param historyPath path of the history endpoint; defaults to {@code /api/v2/stats/history}
 * @param historyDays history window in days (the spec's "last 4 weeks"); defaults to 28
 * @param qualities quality levels queried in one request; defaults to {@code "1,2,3,4"}
 * @param connectTimeoutMs connect timeout; defaults to 2000 ms
 * @param readTimeoutMs read timeout; defaults to 5000 ms
 */
@ConfigurationProperties(prefix = "price")
public record PriceProperties(
    String baseUrl,
    String historyPath,
    int historyDays,
    String qualities,
    int connectTimeoutMs,
    int readTimeoutMs
) {

    public PriceProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://europe.albion-online-data.com" : baseUrl;
        historyPath = historyPath == null || historyPath.isBlank() ? "/api/v2/stats/history" : historyPath;
        qualities = qualities == null || qualities.isBlank() ? "1,2,3,4" : qualities;
        if (historyDays <= 0) {
            historyDays = 28;
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 2000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 5000;
        }
    }
}