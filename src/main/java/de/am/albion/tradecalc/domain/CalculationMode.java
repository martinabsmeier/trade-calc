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
package de.am.albion.tradecalc.domain;

import lombok.Getter;

/**
 * Strategy for selecting the city in which each crafting step is executed.
 *
 * <p>The choice determines how aggressively the calculator may deviate from the
 * user's home city in order to maximise profit.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Getter
public enum CalculationMode {

    /**
     * All actions — buying, refining and crafting — happen in the user's selected
     * home city. Simplest mode; ignores any refining or crafting bonuses in other cities.
     */
    LOCAL_ONLY("Alles in der Heimatstadt"),

    /**
     * Raw materials are bought in the user's home city (so the player uses their
     * own market stocks), but each refining and crafting step is performed in the
     * city that offers the best bonus for that step.
     */
    LOCAL_BUY_BEST_REST("Rohstoffe kaufen in Heimatstadt, Rest optimal"),

    /**
     * Every step — buying, refining and crafting — is performed in whichever
     * city yields the lowest total cost / highest profit. Maximises profit but
     * ignores the player's physical location.
     */
    BEST_OF_ALL("Optimal pro Stufe");

    private final String description;

    CalculationMode(String description) {
        this.description = description;
    }
}