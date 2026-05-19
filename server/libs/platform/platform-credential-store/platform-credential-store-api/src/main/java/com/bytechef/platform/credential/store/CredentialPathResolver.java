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

package com.bytechef.platform.credential.store;

import org.jspecify.annotations.Nullable;

/**
 * Resolves a path template against context variables. Used by external credential store adapters to compute the secret
 * name / vault path from operator-configured templates.
 *
 * <p>
 * Supported placeholders:
 * <ul>
 * <li>{@code {tenant}} — current tenant identifier</li>
 * <li>{@code {env}} — connection environment (production / staging / development), lowercased</li>
 * <li>{@code {ref}} — value of {@code connection.credentialRef}</li>
 * </ul>
 *
 * <p>
 * Throws {@link IllegalArgumentException} if a referenced placeholder has no value supplied.
 *
 * @author Ivica Cardic
 */
public final class CredentialPathResolver {

    private CredentialPathResolver() {
    }

    public static String resolve(
        String template, @Nullable String tenant, @Nullable String environment, @Nullable String ref) {

        String result = template;

        if (template.contains("{tenant}")) {
            if (tenant == null) {
                throw new IllegalArgumentException(
                    "Path template references {tenant} but no tenant is available");
            }

            result = result.replace("{tenant}", tenant);
        }

        if (template.contains("{env}")) {
            if (environment == null) {
                throw new IllegalArgumentException(
                    "Path template references {env} but no environment is available");
            }

            result = result.replace("{env}", environment);
        }

        if (template.contains("{ref}")) {
            if (ref == null) {
                throw new IllegalArgumentException(
                    "Path template references {ref} but no credentialRef is available");
            }

            result = result.replace("{ref}", ref);
        }

        return result;
    }
}
