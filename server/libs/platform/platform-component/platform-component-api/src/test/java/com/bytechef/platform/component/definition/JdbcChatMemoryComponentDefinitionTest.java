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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

/**
 * Pins the ITERATION ORDER of the action cluster-element-type map, not just its contents — see
 * {@link VectorStoreComponentDefinitionTest} for why order is the thing under test here.
 *
 * @author Ivica Cardic
 */
class JdbcChatMemoryComponentDefinitionTest {

    @Test
    void testActionClusterElementTypesIterationOrderIsDeterministic() {
        JdbcChatMemoryComponentDefinition jdbcChatMemoryComponentDefinition =
            mock(JdbcChatMemoryComponentDefinition.class, CALLS_REAL_METHODS);

        assertThat(jdbcChatMemoryComponentDefinition.getActionClusterElementTypes()
            .keySet())
                .containsExactly("addMessages", "getMessages", "deleteConversation", "listConversations");
    }
}
