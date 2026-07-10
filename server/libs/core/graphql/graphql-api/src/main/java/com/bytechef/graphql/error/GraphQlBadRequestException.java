/*
 * Copyright 2025 ByteChef
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

package com.bytechef.graphql.error;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Throw from a GraphQL controller when the caller's input is malformed (e.g. unparseable IDs, missing required fields).
 * The global resolver translates this to {@code ErrorType.BAD_REQUEST} so clients see a 4xx-equivalent classification
 * instead of the default {@code INTERNAL_ERROR}.
 *
 * <p>
 * The {@code code} slot carries a machine-readable discriminator (e.g. {@code UNPARSEABLE_ID}, {@code MISSING_FIELD},
 * {@code ENUM_OUT_OF_RANGE}) so the client can branch on a stable identifier rather than scraping message text. The
 * {@code extensions} map is copied into the GraphQL error payload's {@code extensions} field unchanged — use it for
 * field-path, offending value, or similar structured context.
 *
 * @author Ivica Cardic
 */
public class GraphQlBadRequestException extends RuntimeException {

    public static final String DEFAULT_CODE = "BAD_REQUEST";

    private final String code;
    private final Map<String, Object> extensions;

    public GraphQlBadRequestException(String message) {
        this(DEFAULT_CODE, message, Map.of(), null);
    }

    public GraphQlBadRequestException(String message, Throwable cause) {
        this(DEFAULT_CODE, message, Map.of(), cause);
    }

    public GraphQlBadRequestException(String code, String message, Map<String, Object> extensions) {
        this(code, message, extensions, null);
    }

    public GraphQlBadRequestException(String code, String message, Map<String, Object> extensions, Throwable cause) {
        super(message, cause);

        this.code = Objects.requireNonNull(code, "code");
        this.extensions = Map.copyOf(Objects.requireNonNullElse(extensions, Collections.emptyMap()));
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }
}
