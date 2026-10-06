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
package de.am.albion.tradecalc.dataprovider.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.am.albion.tradecalc.domain.model.Recipe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Thin wrapper around Jackson's {@link ObjectMapper} so {@link RecipeLoader}
 * stays independent of the JSON library and is easy to mock in tests.
 *
 * @author Martin Absmeier
 * @since 1.0.0
 */
@Slf4j
@Component
public class RecipeJsonMapper {

    private final ObjectMapper jackson = new ObjectMapper();

    /**
     * Deserialises a JSON input stream into a {@link Recipe} array.
     *
     * @param in the input stream (not closed by this method)
     * @return the parsed recipes
     * @throws IOException if the JSON cannot be parsed
     */
    public Recipe[] readList(InputStream in) throws IOException {
        log.debug("Parsing recipe JSON stream");
        return jackson.readValue(in, Recipe[].class);
    }
}