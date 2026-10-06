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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@link RecipeLoadException}: verifies constructors preserve
 * their message and cause arguments.
 */
class RecipeLoadExceptionTest {

    @Test
    void messageOnlyConstructor_preservesMessage() {
        RecipeLoadException ex = new RecipeLoadException("missing");
        assertThat(ex.getMessage()).isEqualTo("missing");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void messageAndCauseConstructor_preservesBoth() {
        Throwable cause = new IllegalStateException("boom");
        RecipeLoadException ex = new RecipeLoadException("wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}