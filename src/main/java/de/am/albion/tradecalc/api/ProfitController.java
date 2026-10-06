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

import de.am.albion.tradecalc.api.dto.CalculationRequestDto;
import de.am.albion.tradecalc.api.dto.CalculationResponseDto;
import de.am.albion.tradecalc.api.dto.ProfitEntryDto;
import de.am.albion.tradecalc.service.PriceServiceAdapter;
import de.am.albion.tradecalc.service.ProfitQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint that ranks every known recipe by profit under a chosen
 * {@link de.am.albion.tradecalc.domain.CalculationMode}.
 *
 * <p>Example: <code>POST /api/v1/profit/calculate</code> with body
 * <code>{"mode":"BEST_OF_ALL","homeCity":"Lymhurst","top":20,"includePlan":false}</code>
 * returns the top 20 items ranked by profit.</p>
 *
 * <p>The market prices are sourced from the production
 * {@link PriceServiceAdapter}; the controller stays free of caching details.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/api/v1/profit", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProfitController {

    private final ProfitQueryService queryService;
    private final PriceServiceAdapter prices;

    /**
     * Calculates profit for every known recipe, ranks them, and returns the
     * top entries.
     *
     * @param request the validated request body
     * @return the ranked response; entries may be empty if no recipe yields a
     *         feasible profit under the chosen mode
     */
    @PostMapping(value = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CalculationResponseDto calculate(@Valid @RequestBody CalculationRequestDto request) {
        List<ProfitEntryDto> items = queryService
                .rankTop(request.mode(), request.homeCity(), prices, request.top(), request.includePlan())
                .stream()
                .map(ProfitEntryDto::from)
                .toList();
        return new CalculationResponseDto(
                request.mode(), request.homeCity(), request.includePlan(), items.size(), items);
    }
}