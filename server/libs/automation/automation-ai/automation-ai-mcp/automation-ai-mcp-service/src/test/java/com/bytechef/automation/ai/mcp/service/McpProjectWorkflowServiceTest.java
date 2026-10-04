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

package com.bytechef.automation.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpProjectWorkflowServiceTest {

    @Test
    void testNoUnparameterisedReadExists() {
        assertThat(McpProjectWorkflowService.class.getDeclaredMethods())
            .as("an unparameterised read cannot be guarded by id and would return every row in the table")
            .noneMatch(method -> method.getName()
                .equals("getMcpProjectWorkflows") && method.getParameterCount() == 0);
        assertThat(McpProjectWorkflowServiceImpl.class.getDeclaredMethods())
            .as("an unparameterised read cannot be guarded by id and would return every row in the table")
            .noneMatch(method -> method.getName()
                .equals("getMcpProjectWorkflows") && method.getParameterCount() == 0);
    }
}
