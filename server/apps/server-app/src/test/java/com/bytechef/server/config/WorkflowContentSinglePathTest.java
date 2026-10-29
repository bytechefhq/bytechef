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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.config.ToolCallbackContributorConfiguration;
import com.bytechef.ai.mcp.server.config.ManagementMcpServerConfiguration;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.ee.embedded.ai.copilot.config.EmbeddedCopilotMcpContributorConfiguration;
import java.lang.reflect.Executable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Guards the one-path-per-capability rule for workflow content across every surface that registers the
 * {@code buildWorkflow} intelligent tool: the Projects Copilot panel, the management MCP server and the AI Hub BUILD
 * tool-search catalog.
 *
 * <p>
 * {@link ProjectWorkflowTools} exposes {@code updateWorkflow} and a {@code createProjectWorkflow} that accepts a
 * complete definition, so a surface injecting it could write workflow content without {@code buildWorkflow}'s procedure
 * behind the write. Those surfaces take {@code ProjectWorkflowLifecycleTools} instead; only the workflow-authoring
 * subagents register {@link ProjectWorkflowTools} whole. The check spans three modules, which is why it lives here,
 * where all of them share one classpath.
 * </p>
 *
 * @author Ivica Cardic
 */
class WorkflowContentSinglePathTest {

    private static final List<Class<?>> BUILD_WORKFLOW_SURFACES = List.of(
        ManagementMcpServerConfiguration.class, ToolCallbackContributorConfiguration.class,
        EmbeddedCopilotMcpContributorConfiguration.class);

    @Test
    void testNoBuildWorkflowSurfaceInjectsProjectWorkflowTools() {
        List<String> offenders = new ArrayList<>();

        for (Class<?> surface : BUILD_WORKFLOW_SURFACES) {
            Stream.concat(Arrays.stream(surface.getDeclaredConstructors()), Arrays.stream(surface.getDeclaredMethods()))
                .filter(WorkflowContentSinglePathTest::takesProjectWorkflowTools)
                .forEach(executable -> offenders.add(surface.getSimpleName() + "#" + executable.getName()));
        }

        assertThat(offenders)
            .as("these surfaces register buildWorkflow, so they must take ProjectWorkflowLifecycleTools instead")
            .isEmpty();
    }

    private static boolean takesProjectWorkflowTools(Executable executable) {
        if (Arrays.asList(executable.getParameterTypes())
            .contains(ProjectWorkflowTools.class)) {

            return true;
        }

        for (Type type : executable.getGenericParameterTypes()) {
            if (type instanceof ParameterizedType parameterizedType &&
                (parameterizedType.getRawType() == ObjectProvider.class ||
                    parameterizedType.getRawType()
                        .getTypeName()
                        .endsWith("inject.Provider"))
                &&
                Arrays.asList(parameterizedType.getActualTypeArguments())
                    .contains(ProjectWorkflowTools.class)) {

                return true;
            }
        }

        return false;
    }
}
