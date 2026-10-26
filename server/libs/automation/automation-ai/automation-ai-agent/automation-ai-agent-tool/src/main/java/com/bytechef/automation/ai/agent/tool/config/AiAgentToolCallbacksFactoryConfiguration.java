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

package com.bytechef.automation.ai.agent.tool.config;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
public class AiAgentToolCallbacksFactoryConfiguration {

    @Bean
    @ConditionalOnBean(AiAgentFacade.class)
    AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory(
        AiAgentFacade aiAgentFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ObjectProvider<AiSkillFacade> aiSkillFacadeProvider,
        ObjectProvider<WorkspaceKnowledgeBaseFacade> workspaceKnowledgeBaseFacadeProvider) {

        return new AiAgentToolCallbacksFactory(
            aiAgentFacade, clusterElementDefinitionService, aiSkillFacadeProvider.getIfAvailable(),
            workspaceKnowledgeBaseFacadeProvider.getIfAvailable());
    }
}
