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
package de.am.albion.tradecalc.api;

import de.am.albion.tradecalc.domain.model.MarketPrice;
import de.am.albion.tradecalc.service.PriceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint exposing live market prices. Delegates to {@link PriceService}
 * — caching happens transparently below this layer.
 *
 * <p>Example: <code>GET /api/v1/prices/T4_BOW</code> returns one
 * {@link MarketPrice} per configured city.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/api/v1/prices", produces = MediaType.APPLICATION_JSON_VALUE)
public class PriceController {

    private final PriceService priceService;

    /**
     * Creates the controller.
     *
     * @param priceService the price service
     */
    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    /**
     * Returns market prices for {@code itemId} across all configured cities.
     *
     * @param itemId the Albion item identifier (e.g. {@code "T4_BOW"})
     * @return city → {@link MarketPrice}; empty map if the item is unknown
     */
    @GetMapping("/{itemId}")
    public Map<String, MarketPrice> getPrices(@PathVariable String itemId) {
        return priceService.getPrices(itemId);
    }
}