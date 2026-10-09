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
package de.am.tcalc.domain;

import java.math.BigDecimal;

/**
 * One line of a {@link MaterialCost}: one ingredient with its recipe amount, unit price in the
 * city, and the resulting line cost (count × unitPrice, scale 2, HALF_UP). A {@code null} price
 * or cost means the market has no sales data for the item.
 *
 * @param item ingredient item id, e.g. "T5_PLANKS"
 * @param count units required by the recipe
 * @param unitPrice current unit price (quality "Normal") in the city
 * @param lineCost count × unitPrice
 */
public record MaterialCostLine(String item, int count, BigDecimal unitPrice, BigDecimal lineCost) {
}