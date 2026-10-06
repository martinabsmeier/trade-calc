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
package de.am.albion.tradecalc.dataprovider.albion.mapper;

import de.am.albion.tradecalc.dataprovider.albion.dto.MarketJson;
import de.am.albion.tradecalc.domain.model.MarketPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MarketJsonMapper}: focuses on the timestamp-fallback
 * branches and the {@code 0}-price sentinel handling.
 */
class MarketJsonMapperTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED, ZoneOffset.UTC);

    /**
     * When {@code buy_price_max_date} is present and not the epoch, it is used
     * verbatim — regardless of {@code sell_price_min_date}.
     */
    @Test
    void observedAt_prefersBuyPriceMaxDate() {
        Instant buy = Instant.parse("2026-05-01T12:00:00Z");
        Instant sell = Instant.parse("2026-06-01T12:00:00Z");
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(100), BigDecimal.valueOf(95), sell, buy);

        MarketPrice price = new MarketJsonMapper(FIXED_CLOCK).map(json);

        assertThat(price.observedAt()).isEqualTo(buy);
    }

    /**
     * When {@code buy_price_max_date} is the epoch sentinel, the mapper falls
     * back to {@code sell_price_min_date}.
     */
    @Test
    void observedAt_fallsBackToSellPriceMinDateWhenBuyIsEpoch() {
        Instant sell = Instant.parse("2026-06-01T12:00:00Z");
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(100), BigDecimal.valueOf(95), sell, Instant.EPOCH);

        MarketPrice price = new MarketJsonMapper(FIXED_CLOCK).map(json);

        assertThat(price.observedAt()).isEqualTo(sell);
    }

    /**
     * When both timestamps are missing, the mapper falls back to the
     * configured {@link Clock}.
     */
    @Test
    void observedAt_usesClockWhenBothMissing() {
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(100), BigDecimal.valueOf(95), null, null);

        MarketPrice price = new MarketJsonMapper(FIXED_CLOCK).map(json);

        assertThat(price.observedAt()).isEqualTo(FIXED);
    }

    /**
     * A {@code null} buy price is preserved as {@code null} in the domain
     * record — mapper does not invent a value.
     */
    @Test
    void normalise_preservesNullPrice() {
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(100), null, null, null);

        MarketPrice price = new MarketJsonMapper().map(json);

        assertThat(price.sellPriceMin()).isEqualByComparingTo("100");
        assertThat(price.buyPriceMax()).isNull();
    }

    /**
     * A zero price is normalised to {@code null} (zero is the upstream
     * sentinel for "no active order").
     */
    @Test
    void normalise_zeroBecomesNull() {
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null);

        MarketPrice price = new MarketJsonMapper().map(json);

        assertThat(price.sellPriceMin()).isNull();
        assertThat(price.buyPriceMax()).isNull();
    }

    /**
     * When {@code buy_price_max_date} is the epoch sentinel and
     * {@code sell_price_min_date} is also the epoch sentinel, the mapper
     * falls back to the clock.
     */
    @Test
    void observedAt_sellEpochAlso_fallsBackToClock() {
        MarketJson json = new MarketJson("T4_BOW", "Lymhurst", 1,
                BigDecimal.valueOf(100), BigDecimal.valueOf(95),
                Instant.EPOCH, Instant.EPOCH);

        MarketPrice price = new MarketJsonMapper(FIXED_CLOCK).map(json);

        assertThat(price.observedAt()).isEqualTo(FIXED);
    }
}