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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentUtilsGrepToolTest {

    private static final Path AGENT_WORKSPACE = Path.of("public/automation/11/staging/21/default");

    @TempDir
    private Path basedir;

    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);

    @Test
    void testToolDescriptionNamesTheSandboxWorkspace() throws Exception {
        assertThat(applyTool().getToolDefinition()
            .description()).contains("The agent workspace is /workspace.")
                .doesNotContain(basedir.toString());
    }

    @Test
    void testGrepInTheSandboxRootShowsSandboxPaths() throws Exception {
        ToolCallback toolCallback = applyTool();

        Files.writeString(
            Files.createDirectories(basedir.resolve(AGENT_WORKSPACE)
                .resolve("docs"))
                .resolve("notes.txt"),
            "needle");

        String result = toolCallback.call(JsonUtils.write(Map.of("pattern", "needle", "path", "/workspace")));

        assertThat(result).contains("/workspace/docs/notes.txt")
            .doesNotContain(basedir.toString());
    }

    @Test
    void testPathOutsideTheWorkspaceIsDeniedWithoutShowingTheHostPath() throws Exception {
        String result = applyTool().call(JsonUtils.write(Map.of("pattern", "needle", "path", "/workspace/..")));

        assertThat(result).contains("Access denied")
            .doesNotContain(basedir.toString());
    }

    private ToolCallback applyTool() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(actionContext.getJobPrincipalWorkflowId()).thenReturn(21L);
        when(inputParameters.getInteger(anyString())).thenReturn(null);

        ToolCallbackProviderFunction toolCallbackProviderFunction = new AiAgentUtilsGrepTool(
            new AgentWorkspaceResolver(basedir)).clusterElementDefinition.getElement();

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        return toolCallbacks[0];
    }
}
