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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.am.albion.tradecalc.domain.model.CityBonus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * In-memory repository of static {@link CityBonus city bonuses}, loaded once
 * from {@code bonuses.json} on the classpath.
 *
 * <p>Look-ups for individual bonuses are cached under the {@code cityBonuses}
 * cache; the bulk list is held in a {@code final} field because bonuses are
 * reference data that never changes at runtime.</p>
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Repository
public class CityBonusRepository {

    private final List<CityBonus> bonuses;

    /**
     * Creates the production repository by loading {@code bonuses.json} from
     * the classpath.
     *
     * @param resource the classpath resource containing the bonuses JSON
     */
    @Autowired
    public CityBonusRepository(@Value("classpath:bonuses.json") Resource resource) {
        this(loadFromResource(resource));
        log.info("Initialised CityBonusRepository with {} bonuses", this.bonuses.size());
    }

    private CityBonusRepository(List<CityBonus> bonuses) {
        this.bonuses = bonuses;
    }

    private static List<CityBonus> loadFromResource(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return parse(StreamUtils.copyToString(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bonuses.json from classpath", e);
        }
    }

    private static List<CityBonus> parse(String json) {
        try {
            return List.of(new ObjectMapper().readValue(json, CityBonus[].class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse bonuses.json", e);
        }
    }

    /**
     * Test seam: create an isolated instance from an in-memory JSON string
     * without going through the classpath. Not registered as a Spring bean.
     */
    public static CityBonusRepository forTest(String bonusesJson) {
        return new CityBonusRepository(parse(bonusesJson));
    }

    /**
     * Finds the best (highest) bonus a city offers for the given crafting or
     * refining category. Falls back to the city's generic bonus (where
     * {@link CityBonus#category()} is {@code null}) if no specific bonus exists.
     *
     * @param cityName city to search in
     * @param category category to look up; {@code null} selects the generic bonus
     * @return the matching bonus, or {@code null} if the city has none for the category
     */
    @Cacheable("cityBonuses")
    public CityBonus findBestBonus(String cityName, String category) {
        if (cityName == null) {
            return null;
        }
        CityBonus categoryMatch = null;
        CityBonus genericMatch = null;
        for (CityBonus b : bonuses) {
            if (!b.cityName().equalsIgnoreCase(cityName)) {
                continue;
            }
            if (category == null && b.category() == null) {
                return b;
            }
            if (category != null && category.equalsIgnoreCase(b.category())) {
                if (categoryMatch == null || b.bonus().compareTo(categoryMatch.bonus()) > 0) {
                    categoryMatch = b;
                }
            }
            if (b.category() == null) {
                genericMatch = b;
            }
        }
        return categoryMatch != null ? categoryMatch : genericMatch;
    }
}