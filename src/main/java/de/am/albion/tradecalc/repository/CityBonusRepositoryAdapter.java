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
package de.am.albion.tradecalc.repository;

import de.am.albion.tradecalc.domain.model.CityBonus;
import de.am.albion.tradecalc.service.calculator.CityBonusProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Production adapter: exposes {@link CityBonusRepository} as a
 * {@link CityBonusProvider} for the calculator package.
 */
@Component
@RequiredArgsConstructor
public class CityBonusRepositoryAdapter implements CityBonusProvider {

    private final CityBonusRepository repository;

    @Override
    public BigDecimal bonusFor(String cityName, String category) {
        if (cityName == null) {
            return BigDecimal.ZERO;
        }
        CityBonus bonus = repository.findBestBonus(cityName, category);
        return bonus == null ? BigDecimal.ZERO : bonus.bonus();
    }

    @Override
    public String bestCityFor(String category, String fallback) {
        return repository.findBestCity(category, fallback);
    }
}