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

package com.bytechef.component.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDslToolRiskLevelTest {

    @Test
    void testToolCopiesTheActionsRiskLevel() {
        ModifiableActionDefinition actionDefinition = ComponentDsl.action("deleteRecord")
            .title("Delete Record")
            .riskLevel(RiskLevel.CRITICAL);

        ModifiableClusterElementDefinition<ToolFunction> clusterElementDefinition = ComponentDsl.tool(
            actionDefinition);

        assertThat(clusterElementDefinition.getRiskLevel()).contains(RiskLevel.CRITICAL);
    }

    @Test
    void testAnActionWithoutADeclaredRiskLevelProducesAToolWithoutOne() {
        ModifiableClusterElementDefinition<ToolFunction> clusterElementDefinition = ComponentDsl.tool(
            ComponentDsl.action("getRecord")
                .title("Get Record"));

        assertThat(clusterElementDefinition.getRiskLevel()).isEmpty();
    }
}
