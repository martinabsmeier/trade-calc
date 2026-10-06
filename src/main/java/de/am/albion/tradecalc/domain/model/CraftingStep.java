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
package de.am.albion.tradecalc.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * One concrete step in a {@link CraftingPlan}: an action performed in a specific
 * city for a specific item with a specific cost / benefit.
 *
 * @param step the action kind (e.g. {@code "BUY"}, {@code "REFINE"}, {@code "CRAFT"})
 * @param itemId the item the action is performed on
 * @param city the city to perform the action in
 * @param quantity how many items the step yields
 * @param cost the cost of the step (silver)
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Builder
public record CraftingStep(
        String step,
        String itemId,
        String city,
        int quantity,
        BigDecimal cost) {
}