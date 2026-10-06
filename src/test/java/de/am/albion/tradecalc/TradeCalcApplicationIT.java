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
package de.am.albion.tradecalc;

import de.am.albion.tradecalc.config.AlbionApiProperties;
import de.am.albion.tradecalc.config.CacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that bootstrap the full Spring application context and verify
 * that the wiring of beans and configuration properties matches the project's foundation
 * requirements (see design doc {@code trade-calc.md}).
 */
@SpringBootTest
@ActiveProfiles("dev")
class TradeCalcApplicationIT {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AlbionApiProperties albionApiProperties;

    @Autowired
    private CacheProperties cacheProperties;

    /**
     * Verifies that the application context starts up and that the central
     * beans ({@link AlbionApiProperties}, {@link CacheProperties},
     * {@link CacheManager}) are wired and autowirable.
     */
    @Test
    void applicationContext_isWiredCorrectly() {
        assertThat(albionApiProperties).isNotNull();
        assertThat(cacheProperties).isNotNull();
        assertThat(cacheManager).isNotNull();
    }

    /**
     * Verifies that {@code albion.api.*} values defined in {@code application.yml}
     * are correctly bound to {@link AlbionApiProperties}.
     */
    @Test
    void albionApiProperties_loadValuesFromApplicationYml() {
        assertThat(albionApiProperties.getBaseUrl())
                .isEqualTo("https://west.albion-online-data.com");
        assertThat(albionApiProperties.getServer()).isEqualTo("europe");
        assertThat(albionApiProperties.getLocations())
                .contains("Lymhurst", "Fort Sterling", "Martlock");
        assertThat(albionApiProperties.getQualities()).contains(1);
    }

    /**
     * Verifies that {@code cache.market.*} values defined in {@code application.yml}
     * are correctly bound to {@link CacheProperties}.
     */
    @Test
    void cacheProperties_loadValuesFromApplicationYml() {
        assertThat(cacheProperties.getRefreshInterval().toMinutes()).isEqualTo(5);
        assertThat(cacheProperties.getTtl().toMinutes()).isEqualTo(15);
        assertThat(cacheProperties.getMaxItems()).isEqualTo(5000);
    }

    /**
     * Verifies that the {@link CacheManager} exposes all caches that the application
     * expects (market prices, recipes, city bonuses).
     */
    @Test
    void cacheManager_containsAllConfiguredCaches() {
        assertThat(cacheManager.getCacheNames())
                .contains("marketPrices", "recipes", "cityBonuses");
    }
}