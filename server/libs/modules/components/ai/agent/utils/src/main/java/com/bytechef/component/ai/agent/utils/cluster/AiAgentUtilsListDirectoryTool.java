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

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.nio.file.Path;
import org.springaicommunity.agent.tools.ListDirectoryTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides directory listing for the AI agent.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsListDirectoryTool {

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("listDirectoryTool")
            .title("List Directory Tool")
            .description("List files and directories within a path.")
            .type(TOOLS)
            .object(() -> AiAgentUtilsListDirectoryTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        ListDirectoryTool listDirectoryTool = ListDirectoryTool.builder()
            .workingDirectory(Path.of(System.getProperty("user.dir")))
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(listDirectoryTool));
    }
}
