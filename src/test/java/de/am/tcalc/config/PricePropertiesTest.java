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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * The defaults live in {@code application.yml} — the record carries no fallbacks. This test binds
 * the same values the yml ships and guards them against silent drift.
 */
class PricePropertiesTest {

    private PriceProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
            .bind("price", Bindable.of(PriceProperties.class)).get();
    }

    private static final Map<String, String> YML_DEFAULTS = Map.of(
        "price.base-url", "https://europe.albion-online-data.com",
        "price.history-path", "/api/v2/stats/history",
        "price.history-days", "28",
        "price.qualities", "1,2,3,4",
        "price.connect-timeout-ms", "2000",
        "price.read-timeout-ms", "5000");

    @Test
    void applicationYmlValuesBindAsExpected() {
        PriceProperties p = bind(YML_DEFAULTS);

        assertThat(p.baseUrl()).isEqualTo("https://europe.albion-online-data.com");
        assertThat(p.historyPath()).isEqualTo("/api/v2/stats/history");
        assertThat(p.historyDays()).isEqualTo(28);
        assertThat(p.qualities()).isEqualTo("1,2,3,4");
        assertThat(p.connectTimeoutMs()).isEqualTo(2000);
        assertThat(p.readTimeoutMs()).isEqualTo(5000);
    }

    @Test
    void valuesAreOverridableWithoutClassSideFallbacks() {
        // A key set in an environment wins; a MISSING key stays absent — the record
        // does not silently substitute values in code.
        PriceProperties p = bind(Map.of("price.history-days", "7"));

        assertThat(p.historyDays()).isEqualTo(7);
        assertThat(p.baseUrl()).isNull();
        assertThat(p.historyPath()).isNull();
    }
}