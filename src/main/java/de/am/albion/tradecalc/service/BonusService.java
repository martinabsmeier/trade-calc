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
package de.am.albion.tradecalc.service;

import de.am.albion.tradecalc.domain.model.CityBonus;
import de.am.albion.tradecalc.repository.CityBonusRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Thin facade over {@link CityBonusRepository} that exposes the most common
 * look-ups (busy outcomes) as {@link Cacheable @Cacheable} methods so callers
 * benefit from Caffeine caching without touching the repository directly.
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Service
public class BonusService {

    private final CityBonusRepository repository;

    /**
     * Creates a new bonus service.
     *
     * @param repository the underlying bonus repository
     */
    public BonusService(CityBonusRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the best bonus a city offers for the given category.
     *
     * @param city the city name
     * @param category the crafting / refining category; {@code null} for the generic bonus
     * @return the best bonus or {@link BigDecimal#ZERO} if the city offers no bonus
     */
    @Cacheable("cityBonuses")
    public BigDecimal getBonus(String city, String category) {
        CityBonus bonus = repository.findBestBonus(city, category);
        return bonus == null ? BigDecimal.ZERO : bonus.bonus();
    }
}