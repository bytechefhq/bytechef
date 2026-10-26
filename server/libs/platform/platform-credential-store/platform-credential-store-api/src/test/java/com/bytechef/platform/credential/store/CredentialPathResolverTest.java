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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CredentialPathResolverTest {

    @Test
    void testResolveSimpleTemplate() {
        String result = CredentialPathResolver.resolve("bytechef/{ref}", null, null, "abc-123");

        assertThat(result).isEqualTo("bytechef/abc-123");
    }

    @Test
    void testResolveWithTenantAndEnvironment() {
        String result = CredentialPathResolver.resolve(
            "bytechef/{tenant}/{env}/{ref}", "acme-corp", "production", "abc-123");

        assertThat(result).isEqualTo("bytechef/acme-corp/production/abc-123");
    }

    @Test
    void testResolveWithMissingTenantPlaceholderThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{tenant}/{ref}", null, null, "abc-123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenant");
    }

    @Test
    void testResolveWithMissingEnvironmentPlaceholderThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{env}/{ref}", "acme", null, "abc-123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("env");
    }

    @Test
    void testResolveWithMissingRefThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{ref}", null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ref");
    }

    @Test
    void testResolveLeavesNonTemplateLiteralsUnchanged() {
        String result = CredentialPathResolver.resolve("secret/data/bytechef/connections/{ref}", null, null, "uuid");

        assertThat(result).isEqualTo("secret/data/bytechef/connections/uuid");
    }
}
