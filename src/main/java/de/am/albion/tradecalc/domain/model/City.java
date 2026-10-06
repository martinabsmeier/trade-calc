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

/**
 * A Royal City in Albion Online that can be used as a crafting or refining
 * location. Cities have a fixed set of {@link CityBonus bonuses} per category.
 *
 * @param name the city's display name (e.g. {@code "Lymhurst"})
 * @author Martin Absmeier
 * @since 1.0.0
 */
public record City(String name) {
}