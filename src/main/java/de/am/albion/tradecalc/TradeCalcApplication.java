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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the <strong>trade-calc</strong> application.
 *
 * <p>trade-calc is a profit calculator for crafted items in Albion Online. It determines the
 * most profitable crafting route for every craftable item by combining Royal City bonuses for
 * refining and crafting with live market data (buy / sell prices) from the
 * <a href="https://www.albion-online-data.com/api/">Albion Online Data API</a>.</p>
 *
 * <p>The application is bootstrapped with the following Spring features enabled:
 * <ul>
 *     <li>{@link EnableCaching} — activates Caffeine-based caching for market prices,
 *         recipes and city bonuses.</li>
 *     <li>{@link EnableScheduling} — required for the periodic background refresh of
 *         market data (interval configured via {@code cache.market.refresh-interval}).</li>
 *     <li>{@link EnableConfigurationProperties} — binds the externalised
 *         {@code albion.api.*} and {@code cache.market.*} configuration blocks from
 *         {@code application.yml} to {@link AlbionApiProperties} and {@link CacheProperties}.</li>
 * </ul>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({AlbionApiProperties.class, CacheProperties.class})
public class TradeCalcApplication {

    /**
     * Bootstraps the Spring Boot environment and launches the embedded servlet container.
     *
     * @param args command-line arguments forwarded to {@link SpringApplication#run(Class, String...)}.
     */
    public static void main(String[] args) {
        SpringApplication.run(TradeCalcApplication.class, args);
    }
}