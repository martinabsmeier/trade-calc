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
package de.am.albion.tradecalc.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.am.albion.tradecalc.domain.model.CraftingPlan;
import de.am.albion.tradecalc.domain.model.ProfitResult;
import de.am.albion.tradecalc.service.ProfitQueryService;

/**
 * One row of the {@link CalculationResponseDto}: the calculated profit and,
 * when requested, the crafting plan.
 *
 * <p>{@code plan} is serialised only when present — Jackson's
 * {@code NON_NULL} inclusion keeps the response small by default.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfitEntryDto(
        String itemId,
        ProfitResult profit,
        CraftingPlan plan) {

    /**
     * Adapts the service-layer {@link ProfitQueryService.ProfitEntry} to the
     * DTO shape, flattening {@code Optional<String> plan()} to {@code null}.
     */
    public static ProfitEntryDto from(ProfitQueryService.ProfitEntry entry) {
        return new ProfitEntryDto(entry.itemId(), entry.profit(), entry.planOrNull());
    }
}