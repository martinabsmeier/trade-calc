/*
 * Copyright 2026 Martin Absmeier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.am.tcalc;

import static org.assertj.core.api.Assertions.assertThat;

import de.am.tcalc.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full Spring context smoke test. Catches wiring regressions (cache manager, Jackson, log4j2) and
 * also covers the {@link de.am.tcalc.TradeCalcApplication} entry point so the coverage
 * gate does not break the build as soon as new classes are added under the root package.
 */
@SpringBootTest
class TradeCalcApplicationIT {

    @Autowired
    RecipeService recipeService;

    @Test
    void contextLoadsAndRecipesAreAvailable() {
        // ponytail: dynamische Größe statt Snapshot (Dump-Regeneration ändert die Zahl).
        assertThat(recipeService.all().size()).isGreaterThan(6000);
        assertThat(recipeService.findByItemId("T4_2H_LONGBOW")).isPresent();
    }
}
