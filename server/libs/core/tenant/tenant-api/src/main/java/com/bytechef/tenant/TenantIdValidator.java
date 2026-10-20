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

package com.bytechef.tenant;

import java.util.regex.Pattern;

/**
 * Validates tenant identifiers and tenant-derived database schema names against a strict, SQL-identifier-safe charset
 * ({@code ^[a-zA-Z0-9_]+$}). Tenant ids flow into {@code SET search_path} statements that PostgreSQL cannot
 * parameterize, so whitelisting the charset is the SQL-injection mitigation.
 *
 * @author Ivica Cardic
 */
public final class TenantIdValidator {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private TenantIdValidator() {
    }

    public static boolean isValid(String tenantId) {
        return tenantId != null && VALID_PATTERN.matcher(tenantId)
            .matches();
    }

    public static void validate(String tenantId) {
        if (!isValid(tenantId)) {
            throw new IllegalArgumentException(
                "Invalid tenant ID. Must contain only alphanumeric characters and underscores.");
        }
    }

    public static void validateDatabaseSchema(String schema) {
        if (!isValid(schema)) {
            throw new IllegalArgumentException(
                "Invalid database schema name '" + schema + "'. Must contain only alphanumeric characters and " +
                    "underscores.");
        }
    }
}
