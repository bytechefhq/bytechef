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

package com.bytechef.component.ai.agent.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/ai_agent-utils_v1.json", createComponentHandler(true).getDefinition());
    }

    @Test
    public void testGetComponentDefinitionLeavesOutShellToolsUnlessEnabled() {
        assertThat(clusterElementNames(createComponentHandler(false))).doesNotContain("shellTools")
            .contains("fileSystemTools");
        assertThat(clusterElementNames(createComponentHandler(true))).contains("shellTools");
    }

    private static List<String> clusterElementNames(AiAgentUtilsComponentHandler componentHandler) {
        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        Optional<List<ClusterElementDefinition<?>>> clusterElements =
            componentDefinition.getClusterElements();

        return clusterElements.orElseThrow()
            .stream()
            .map(ClusterElementDefinition::getName)
            .toList();
    }

    private static AiAgentUtilsComponentHandler createComponentHandler(boolean shellEnabled) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai.AgentUtils agentUtils = applicationProperties.getAi()
            .getAgentUtils();

        agentUtils.getShell()
            .setEnabled(shellEnabled);
        agentUtils.getWorkspace()
            .setBasedir("build/ai-agent-workspaces");

        return new AiAgentUtilsComponentHandler(
            Mockito.mock(AiAgentToolFacade.class), Mockito.mock(AiSkillFacade.class), applicationProperties,
            List.of(), null, null, null, null);
    }
}
