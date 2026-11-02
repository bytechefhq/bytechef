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

package com.bytechef.component.a2a;

import static com.bytechef.component.a2a.constant.A2aConstants.A2A;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.a2a.cluster.A2aAgentTool;
import com.bytechef.component.a2a.connection.A2aConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("a2a_v1_ComponentHandler")
public class A2aComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public A2aComponentHandler(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.AgentUtils agentUtils = applicationProperties.getAi()
            .getAgentUtils();

        A2aAgentTool a2aAgentTool = new A2aAgentTool(getAllowedPrivateHosts(agentUtils));

        this.componentDefinition = component(A2A)
            .title("A2A")
            .description("Delegate tasks to remote agents over the A2A (Agent2Agent) protocol.")
            .icon("path:assets/a2a.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(A2aConnection.CONNECTION_DEFINITION)
            .clusterElements(a2aAgentTool.clusterElementDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static Set<String> getAllowedPrivateHosts(ApplicationProperties.Ai.AgentUtils agentUtils) {
        List<String> allowedPrivateHosts = agentUtils.getAllowedPrivateHosts();

        if (allowedPrivateHosts == null) {
            return Set.of();
        }

        return Set.copyOf(allowedPrivateHosts);
    }
}
