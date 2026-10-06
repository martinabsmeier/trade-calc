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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/**
 * Maps an upstream {@link MarketJson} record to the domain {@link MarketPrice}
 * record.
 *
 * <p>The Albion API encodes the absence of buy / sell orders as a price of
 * {@code 0}. This mapper translates such sentinels to {@code null} so callers
 * downstream never have to special-case zero prices. When both timestamps are
 * missing, {@link #map(MarketJson) map} falls back to {@link Instant#now()} via
 * the supplied {@link Clock} (defaults to {@link Clock#systemUTC()}).</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Component
public class MarketJsonMapper {

    private final Clock clock;

    /**
     * Creates a mapper that uses the system UTC clock as fallback timestamp.
     */
    public MarketJsonMapper() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a mapper with an explicit clock. Test seam — production code uses
     * the no-arg constructor.
     *
     * @param clock the clock used to fill missing timestamps
     */
    public MarketJsonMapper(Clock clock) {
        this.clock = clock;
    }

    /**
     * Converts a single upstream record to a domain record.
     *
     * @param json the upstream record; must not be {@code null}
     * @return the equivalent {@link MarketPrice}, with {@code 0} prices
     *         normalised to {@code null}
     */
    public MarketPrice map(MarketJson json) {
        return new MarketPrice(
                json.itemId(),
                json.city(),
                json.quality(),
                normalise(json.buyPriceMax()),
                normalise(json.sellPriceMin()),
                observedAt(json));
    }

    private static BigDecimal normalise(BigDecimal price) {
        if (price == null || price.signum() == 0) {
            return null;
        }
        return price;
    }

    private Instant observedAt(MarketJson json) {
        if (json.buyPriceMaxDate() != null && !json.buyPriceMaxDate().equals(Instant.EPOCH)) {
            return json.buyPriceMaxDate();
        }
        if (json.sellPriceMinDate() != null && !json.sellPriceMinDate().equals(Instant.EPOCH)) {
            return json.sellPriceMinDate();
        }
        return clock.instant();
    }
}