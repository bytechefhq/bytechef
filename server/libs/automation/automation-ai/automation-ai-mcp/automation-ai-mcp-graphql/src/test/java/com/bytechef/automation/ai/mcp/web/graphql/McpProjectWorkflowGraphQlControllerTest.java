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

package com.bytechef.automation.ai.mcp.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;

/**
 * @author Ivica Cardic
 */
class McpProjectWorkflowGraphQlControllerTest {

    @Test
    void testUnguardableRootQueriesAreNotExposed() {
        assertThat(McpProjectWorkflowGraphQlController.class.getDeclaredMethods())
            .noneMatch(
                method -> method.isAnnotationPresent(QueryMapping.class) &&
                    (method.getName()
                        .equals("mcpProjectWorkflows") ||
                        method.getName()
                            .equals("mcpProjectWorkflowsByProjectDeploymentWorkflowId")));
    }
}
