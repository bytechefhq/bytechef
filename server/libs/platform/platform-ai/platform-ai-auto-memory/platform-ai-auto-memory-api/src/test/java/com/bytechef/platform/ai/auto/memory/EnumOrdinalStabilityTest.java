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

package com.bytechef.platform.ai.auto.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testAiAutoMemoryTypeOrdinalsAreStable() {
        assertThat(AiAutoMemoryType.values()).startsWith(
            AiAutoMemoryType.USER, AiAutoMemoryType.FEEDBACK, AiAutoMemoryType.PROJECT, AiAutoMemoryType.REFERENCE);
    }

    @Test
    void testAiAutoMemoryPrincipalTypeOrdinalsAreStable() {
        assertThat(AiAutoMemoryPrincipalType.values()).startsWith(
            AiAutoMemoryPrincipalType.USER, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT,
            AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE);
    }

    @Test
    void testEnvironmentOrdinalsAreStable() {
        assertThat(Environment.values()).startsWith(
            Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION);
    }
}
