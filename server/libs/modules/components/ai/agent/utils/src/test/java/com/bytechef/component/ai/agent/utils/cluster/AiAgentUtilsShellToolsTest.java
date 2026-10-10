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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@DisabledOnOs(OS.WINDOWS)
class AiAgentUtilsShellToolsTest {

    @TempDir
    private Path basedir;

    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);

    @Test
    void testBashStartsInTheAgentWorkspace() throws Exception {
        String result = callBash("pwd");

        assertThat(result).contains(
            basedir.resolve("public/automation/11/staging/21/default")
                .toRealPath()
                .toString());
    }

    @Test
    void testBashDoesNotSeeTheServerEnvironment() throws Exception {
        String serverVariable = System.getenv()
            .keySet()
            .stream()
            .filter(name -> !Map.of("HOME", "", "PATH", "", "TMPDIR", "", "PWD", "", "SHLVL", "", "_", "")
                .containsKey(name))
            .findFirst()
            .orElseThrow();

        String result = callBash("echo \"value=${" + serverVariable + ":-unset}\"");

        assertThat(result).contains("value=unset");
    }

    private String callBash(String command) throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(actionContext.getJobPrincipalWorkflowId()).thenReturn(21L);

        ToolCallbackProviderFunction toolCallbackProviderFunction = new AiAgentUtilsShellTools(
            new AgentWorkspaceResolver(basedir)).clusterElementDefinition.getElement();

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        ToolCallback bashToolCallback = Arrays.stream(toolCallbacks)
            .filter(toolCallback -> "Bash".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        return bashToolCallback.call(JsonUtils.write(Map.of("command", command)));
    }
}
