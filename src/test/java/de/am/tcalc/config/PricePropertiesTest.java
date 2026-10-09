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

import org.junit.jupiter.api.Test;

class PricePropertiesTest {

    @Test
    void emptyFieldsFallBackToSpecDefaults() {
        PriceProperties p = new PriceProperties(null, " ", 0, "", -5, -5);

        assertThat(p.baseUrl()).isEqualTo("https://europe.albion-online-data.com");
        assertThat(p.historyPath()).isEqualTo("/api/v2/stats/history");
        assertThat(p.historyDays()).isEqualTo(28);
        assertThat(p.qualities()).isEqualTo("1,2,3,4");
        assertThat(p.connectTimeoutMs()).isEqualTo(2000);
        assertThat(p.readTimeoutMs()).isEqualTo(5000);
    }

    @Test
    void explicitValuesAreKept() {
        PriceProperties p = new PriceProperties(
            "https://west.albion-online-data.com", "/api/v2/stats/history", 7, "1", 100, 200);

        assertThat(p.baseUrl()).isEqualTo("https://west.albion-online-data.com");
        assertThat(p.historyDays()).isEqualTo(7);
        assertThat(p.qualities()).isEqualTo("1");
        assertThat(p.connectTimeoutMs()).isEqualTo(100);
        assertThat(p.readTimeoutMs()).isEqualTo(200);
    }
}