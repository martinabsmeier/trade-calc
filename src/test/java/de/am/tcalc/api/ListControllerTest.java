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
package de.am.tcalc.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.am.tcalc.domain.ItemPriceStats;
import de.am.tcalc.domain.MaterialCost;
import de.am.tcalc.domain.MaterialCostLine;
import de.am.tcalc.service.ListQueryService;
import de.am.tcalc.service.ListQueryService.ListRow;
import de.am.tcalc.service.PriceService;
import de.am.tcalc.service.calculator.MaterialCostService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ListController.class)
class ListControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ListQueryService listQueryService;
    @MockBean PriceService priceService;
    @MockBean MaterialCostService materialCostService;

    private final ListRow row = new ListRow(
        "T4_2H_LONGBOW", "Langbogen des Adepten", 4, 0,
        new BigDecimal("10.0"), new BigDecimal("5329.00"));

    @BeforeEach
    void stubServices() {
        when(listQueryService.categories()).thenReturn(Set.of("Bögen", "Essen"));
        when(listQueryService.subcategories(anyString())).thenReturn(Set.of("Bögen", "Langbögen"));
        when(listQueryService.rows(anyString(), anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(List.of(row));
        when(priceService.stat(anyString(), anyString())).thenReturn(new ItemPriceStats(
            "T4_2H_LONGBOW", new BigDecimal("10.0"), Map.of(1, new BigDecimal("5329.00"))));
        when(materialCostService.cost(anyString(), org.mockito.ArgumentMatchers.any(), anyString()))
            .thenAnswer(inv -> new MaterialCost(inv.getArgument(0), inv.getArgument(1),
                inv.getArgument(2),
                List.of(new MaterialCostLine("T4_PLANKS", 32, new BigDecimal("10.00"),
                    new BigDecimal("320.00"))),
                new BigDecimal("320.00")));
    }

    @Test
    void shellRendersAllDropdowns() throws Exception {
        MvcResult result = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andReturn();
        String html = result.getResponse().getContentAsString();

        assertThat(html).contains("Fort Sterling", "Schwarzer Markt");     // 8 spec cities
        assertThat(html).contains("Bögen", "Essen");                       // categories
        assertThat(html).contains("Normal", "Außergewöhnlich");            // quality labels
        assertThat(html).contains("js/htmx.min.js");                       // local script, no CDN
        assertThat(html).contains("hx-get=\"/list\"");                     // HTMX wiring
    }

    @Test
    void listFragmentRendersRowAndDefaults() throws Exception {
        // No params → controller defaults: Lymhurst / Alle / quality 1 / size 25
        MvcResult result = mvc.perform(get("/list"))
            .andExpect(status().isOk())
            .andReturn();
        String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(html).contains("Langbogen des Adepten");
        assertThat(html).contains("5329.00");                              // price of quality 1
        assertThat(html).contains("hx-get=\"/calc?itemId=T4_2H_LONGBOW\""); // base variant, no @suffix
    }

    @Test
    void calcFragmentRendersMaterialCostAndPendingVariante2() throws Exception {
        MvcResult result = mvc.perform(get("/calc")
                .param("itemId", "T4_2H_LONGBOW").param("enchantmentLevel", "1")
                .param("city", "Thetford").param("quality", "2"))
            .andExpect(status().isOk())
            .andReturn();
        String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(html).contains("T4_2H_LONGBOW@1");                      // market id with @suffix
        assertThat(html).contains("T4_PLANKS", "320.00");                  // material lines + total
        assertThat(html).contains("Thetford");
        assertThat(html).contains("Variante 2");                           // placeholder note
    }
}