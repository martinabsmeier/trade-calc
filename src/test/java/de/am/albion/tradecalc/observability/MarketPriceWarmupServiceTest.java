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
package de.am.albion.tradecalc.observability;

import de.am.albion.tradecalc.dataprovider.recipe.RecipeLoader;
import de.am.albion.tradecalc.domain.model.Recipe;
import de.am.albion.tradecalc.domain.model.RecipeIngredient;
import de.am.albion.tradecalc.service.PriceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-unit tests for {@link MarketPriceWarmupService}. The recipe loader and
 * price service are stubbed so the test does not need a Spring context.
 */
class MarketPriceWarmupServiceTest {

    @Test
    void collectReferencedItems_deduplicatesAndPreservesRoot() {
        RecipeLoader loader = mock(RecipeLoader.class);
        Recipe bow = new Recipe("T4_BOW", 4, "Bows", "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
        Recipe sword = new Recipe("T4_SWORD", 4, "Swords", "Metall",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
        when(loader.loadAll()).thenReturn(List.of(bow, sword));

        MarketPriceWarmupService warmup = new MarketPriceWarmupService(loader, mock(PriceService.class));

        Set<String> items = warmup.collectReferencedItems();

        assertThat(items).containsExactly("T4_BOW", "T4_WOOD", "T4_SWORD");
    }

    @Test
    void warmup_callsGetPricesForEveryItem() {
        RecipeLoader loader = mock(RecipeLoader.class);
        Recipe bow = new Recipe("T4_BOW", 4, "Bows", "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
        when(loader.loadAll()).thenReturn(List.of(bow));

        PriceService priceService = mock(PriceService.class);

        MarketPriceWarmupService warmup = new MarketPriceWarmupService(loader, priceService);
        warmup.warmup();

        verify(priceService).getPrices("T4_BOW");
        verify(priceService).getPrices("T4_WOOD");
    }

    @Test
    void warmup_swallowsExceptionsPerItem() {
        RecipeLoader loader = mock(RecipeLoader.class);
        Recipe bow = new Recipe("T4_BOW", 4, "Bows", "Holz",
                List.of(new RecipeIngredient("T4_WOOD", 1, null)));
        when(loader.loadAll()).thenReturn(List.of(bow));

        PriceService priceService = mock(PriceService.class);
        doThrow(new RuntimeException("upstream down")).when(priceService).getPrices("T4_BOW");

        MarketPriceWarmupService warmup = new MarketPriceWarmupService(loader, priceService);

        // Must NOT throw — a single bad item must not abort the warmup loop.
        warmup.warmup();

        verify(priceService, times(1)).getPrices("T4_BOW");
        verify(priceService, times(1)).getPrices("T4_WOOD");
    }
}