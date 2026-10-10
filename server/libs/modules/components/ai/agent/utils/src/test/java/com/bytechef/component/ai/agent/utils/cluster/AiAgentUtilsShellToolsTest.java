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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.utils.cluster.exec.DockerExecBackendRegistry;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.common.exec.ExecBackend;
import org.springaicommunity.agent.common.exec.ExecResult;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentUtilsShellToolsTest {

    private static final Path AGENT_WORKSPACE = Path.of("public/automation/11/staging/21/default");

    @TempDir
    private Path basedir;

    private final Parameters connectionParameters = mock(Parameters.class);
    private final DockerExecBackendRegistry dockerExecBackendRegistry = mock(DockerExecBackendRegistry.class);
    private final Parameters inputParameters = mock(Parameters.class);

    @Test
    void testBashRunsInTheDockerContainerOfTheAgentWorkspaceByDefault() throws Exception {
        ExecBackend execBackend = mock(ExecBackend.class);

        when(inputParameters.getString(AiAgentUtilsShellTools.EXEC_BACKEND, AiAgentUtilsShellTools.DOCKER))
            .thenReturn(AiAgentUtilsShellTools.DOCKER);
        when(dockerExecBackendRegistry.getExecBackend(basedir.resolve(AGENT_WORKSPACE)))
            .thenReturn(execBackend);
        when(execBackend.run(any())).thenReturn(ExecResult.completed(0, "in container", ""));

        assertThat(callBash(false, "pwd")).contains("in container");

        verify(execBackend).run(any());
    }

    @Test
    void testLocalBackendIsRefusedUnlessTheServerAllowsIt() {
        when(inputParameters.getString(AiAgentUtilsShellTools.EXEC_BACKEND, AiAgentUtilsShellTools.DOCKER))
            .thenReturn(AiAgentUtilsShellTools.LOCAL);

        assertThatThrownBy(() -> callBash(false, "pwd"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void testExecutionBackendOptionsOfferLocalOnlyWhenTheServerAllowsIt() {
        assertThat(execBackendOptionValues(false)).containsExactly(AiAgentUtilsShellTools.DOCKER);
        assertThat(execBackendOptionValues(true))
            .containsExactly(AiAgentUtilsShellTools.DOCKER, AiAgentUtilsShellTools.LOCAL);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testLocalBashStartsInTheAgentWorkspace() throws Exception {
        givenLocalBackend();

        String result = callBash(true, "pwd");

        assertThat(result).contains(
            basedir.resolve(AGENT_WORKSPACE)
                .toRealPath()
                .toString());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testLocalBashDoesNotSeeTheServerEnvironment() throws Exception {
        givenLocalBackend();

        String serverVariable = System.getenv()
            .keySet()
            .stream()
            .filter(name -> !Map.of("HOME", "", "PATH", "", "TMPDIR", "", "PWD", "", "SHLVL", "", "_", "")
                .containsKey(name))
            .findFirst()
            .orElseThrow();

        String result = callBash(true, "echo \"value=${" + serverVariable + ":-unset}\"");

        assertThat(result).contains("value=unset");
    }

    private String callBash(boolean allowLocal, String command) throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(actionContext.getJobPrincipalWorkflowId()).thenReturn(21L);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            createShellTools(allowLocal).clusterElementDefinition.getElement();

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

    private AiAgentUtilsShellTools createShellTools(boolean allowLocal) {
        return new AiAgentUtilsShellTools(new AgentWorkspaceResolver(basedir), dockerExecBackendRegistry, allowLocal);
    }

    private List<String> execBackendOptionValues(boolean allowLocal) {
        List<? extends Property> properties = createShellTools(allowLocal).clusterElementDefinition.getProperties()
            .orElseThrow();

        Property.StringProperty execBackendProperty = properties.stream()
            .filter(property -> AiAgentUtilsShellTools.EXEC_BACKEND.equals(property.getName()))
            .map(Property.StringProperty.class::cast)
            .findFirst()
            .orElseThrow();

        List<? extends Option<String>> options = execBackendProperty.getOptions()
            .orElseThrow();

        return options.stream()
            .map(Option::getValue)
            .toList();
    }

    private void givenLocalBackend() {
        when(inputParameters.getString(AiAgentUtilsShellTools.EXEC_BACKEND, AiAgentUtilsShellTools.DOCKER))
            .thenReturn(AiAgentUtilsShellTools.LOCAL);
    }
}
