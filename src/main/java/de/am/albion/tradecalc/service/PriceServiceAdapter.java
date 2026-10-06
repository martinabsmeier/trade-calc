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
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.domain.model.MarketPrice;
import de.am.albion.tradecalc.service.calculator.PriceLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Production adapter: exposes {@link PriceService} as a
 * {@link PriceLookup} for the calculator package.
 */
@Component
@RequiredArgsConstructor
public class PriceServiceAdapter implements PriceLookup {

    private final PriceService priceService;

    @Override
    public Map<String, MarketPrice> pricesFor(String itemId) {
        return priceService.getPrices(itemId);
    }
}