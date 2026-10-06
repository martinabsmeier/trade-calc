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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AlbionApiProperties}. Verifies that the JavaBean-style
 * setter / getter pair keeps Spring's relaxed binding working as expected and
 * that unset properties remain {@code null}.
 */
class AlbionApiPropertiesTest {

    /**
     * Newly created instances have all fields unset (Spring will later bind them
     * from {@code application.yml}).
     */
    @Test
    void defaultValues_areNullOrEmpty() {
        AlbionApiProperties properties = new AlbionApiProperties();

        assertThat(properties.getBaseUrl()).isNull();
        assertThat(properties.getServer()).isNull();
        assertThat(properties.getLocations()).isNull();
        assertThat(properties.getQualities()).isNull();
    }

    /**
     * Each setter updates the corresponding field and the getter exposes the
     * latest value — the contract Spring's {@code @ConfigurationProperties}
     * binding depends on.
     */
    @Test
    void setters_updateValues() {
        AlbionApiProperties properties = new AlbionApiProperties();

        properties.setBaseUrl("https://west.albion-online-data.com");
        properties.setServer("europe");
        properties.setLocations(List.of("Lymhurst", "Martlock"));
        properties.setQualities(List.of(1, 2, 3));

        assertThat(properties.getBaseUrl()).isEqualTo("https://west.albion-online-data.com");
        assertThat(properties.getServer()).isEqualTo("europe");
        assertThat(properties.getLocations()).containsExactly("Lymhurst", "Martlock");
        assertThat(properties.getQualities()).containsExactly(1, 2, 3);
    }
}