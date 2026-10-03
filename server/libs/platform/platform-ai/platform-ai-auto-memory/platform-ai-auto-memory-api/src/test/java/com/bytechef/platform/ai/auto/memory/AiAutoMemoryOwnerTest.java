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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAutoMemoryOwnerTest {

    @Test
    void testRejectsANonPositiveWorkspaceId() {
        assertThatThrownBy(
            () -> new AiAutoMemoryOwner(0, AiAutoMemoryPrincipalType.USER, 1, Environment.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspaceId");
    }

    @Test
    void testRejectsANonPositivePrincipalId() {
        assertThatThrownBy(
            () -> new AiAutoMemoryOwner(1, AiAutoMemoryPrincipalType.USER, -1, Environment.DEVELOPMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("principalId");
    }

    /**
     * Spring Data JDBC hydration builds the entity with its owner fields at 0 before filling them, so an entity that
     * was never filled cannot pass for a real owner.
     */
    @Test
    void testAnUnhydratedMemoryHasNoOwner() {
        AiAutoMemory memory = new AiAutoMemory();

        assertThatThrownBy(memory::getOwner).isInstanceOf(IllegalArgumentException.class);
    }
}
