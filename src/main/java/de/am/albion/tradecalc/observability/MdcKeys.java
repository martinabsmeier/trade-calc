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

/**
 * Stable names of the MDC (SLF4J mapped diagnostic context) attributes used
 * by the request-id filter. Keeping them in one place stops the rest of the
 * codebase from sprinkling string literals across log calls.
 */
public final class MdcKeys {

    /** Stable per-request correlation id; falls back to a UUID. */
    public static final String REQUEST_ID = "requestId";

    private MdcKeys() {
        // constants only
    }
}