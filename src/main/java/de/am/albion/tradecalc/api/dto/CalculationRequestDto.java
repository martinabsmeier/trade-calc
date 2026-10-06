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

import de.am.albion.tradecalc.domain.CalculationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound payload for {@code POST /api/v1/profit/calculate}.
 *
 * @param mode         the {@link CalculationMode} to apply
 * @param homeCity     the user-selected home city; required for
 *                     {@code LOCAL_ONLY} and {@code LOCAL_BUY_BEST_REST},
 *                     informational otherwise
 * @param top          maximum number of ranked entries to return;
 *                     {@code 0} means "all" (no limit)
 * @param includePlan  when {@code true}, every entry carries its
 *                     {@code CraftingPlan}; defaults to {@code false} to
 *                     keep the response small
 */
public record CalculationRequestDto(
        @NotNull CalculationMode mode,
        @NotBlank String homeCity,
        @Min(0) @Max(500) int top,
        boolean includePlan) {

    /**
     * Bean Validation treats missing JSON keys as absent values; an absent
     * {@code includePlan} therefore means {@code false} (cheap default).
     */
    public CalculationRequestDto {
        if (top < 0) {
            top = 0;
        }
    }
}