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
package de.am.albion.tradecalc.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CacheProperties} and the ISO-8601 {@link Duration} parsing
 * that Spring Boot performs when binding {@code application.yml} values.
 */
class CachePropertiesTest {

    /**
     * The class declares sensible defaults so that a missing or incomplete
     * configuration block in {@code application.yml} still yields a usable bean.
     */
    @Test
    void defaultValues_areUsedWhenNothingIsConfigured() {
        CacheProperties properties = new CacheProperties();

        assertThat(properties.getRefreshInterval()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getMaxItems()).isEqualTo(5000);
    }

    /**
     * Each setter updates the corresponding field and the getter exposes the
     * latest value.
     */
    @Test
    void setters_updateValues() {
        CacheProperties properties = new CacheProperties();

        properties.setRefreshInterval(Duration.ofMinutes(2));
        properties.setTtl(Duration.ofMinutes(30));
        properties.setMaxItems(10000);

        assertThat(properties.getRefreshInterval()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.getMaxItems()).isEqualTo(10000);
    }

    /**
     * Verifies the simple ISO-8601 duration format used in {@code application.yml}
     * ({@code PT5M} = 5 minutes) parses as expected — guards against accidental
     * changes in the formatter.
     */
    @Test
    void parsesIso8601DurationString() {
        Duration parsed = Duration.parse("PT5M");

        assertThat(parsed).isEqualTo(Duration.ofMinutes(5));
    }

    /**
     * Verifies a more complex ISO-8601 duration ({@code PT1H30M} = 1 h 30 min)
     * is parsed correctly.
     */
    @Test
    void parsesComplexIso8601DurationString() {
        Duration parsed = Duration.parse("PT1H30M");

        assertThat(parsed).isEqualTo(Duration.ofMinutes(90));
    }
}