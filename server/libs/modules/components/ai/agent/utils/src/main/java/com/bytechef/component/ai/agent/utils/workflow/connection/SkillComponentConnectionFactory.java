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

package com.bytechef.component.ai.agent.utils.workflow.connection;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSkillsTool;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ClusterElementConnectionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Discovers {@link ComponentConnection} instances for the {@code skillsTool} cluster element by analysing the skill
 * scripts referenced in the cluster element's {@code skills} parameter.
 *
 * <p>
 * For each skill, the factory downloads the skill archive, scans script files for
 * {@code context.component.{name}.{action}(...)} calls, and creates a connection for every component that exists in the
 * registry. Unknown components are silently skipped.
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class SkillComponentConnectionFactory implements ClusterElementConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(SkillComponentConnectionFactory.class);

    private static final String AI_AGENT_UTILS = "aiAgentUtils";
    private static final String SKILLS_TOOL = "skillsTool";
    private static final String SKILLS = "skills";

    private final AiSkillFacade aiSkillFacade;
    private final ComponentDefinitionService componentDefinitionService;

    public SkillComponentConnectionFactory(
        AiSkillFacade aiSkillFacade, ComponentDefinitionService componentDefinitionService) {

        this.aiSkillFacade = aiSkillFacade;
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public boolean supports(String componentName, String clusterElementName) {
        return AI_AGENT_UTILS.equals(componentName) && SKILLS_TOOL.equals(clusterElementName);
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> parameters) {

        List<Long> skillIds = MapUtils.getList(parameters, SKILLS, Long.class, List.of());

        if (skillIds.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueComponentNames = new LinkedHashSet<>();

        for (Long skillId : skillIds) {
            if (skillId == null) {
                continue;
            }

            try {
                byte[] zipBytes = aiSkillFacade.getAiSkillDownload(skillId);

                Map<String, List<AiAgentUtilsSkillsTool.ComponentCall>> scriptAnalysis =
                    AiAgentUtilsSkillsTool.analyzeSkillScripts(zipBytes);

                for (List<AiAgentUtilsSkillsTool.ComponentCall> calls : scriptAnalysis.values()) {
                    for (AiAgentUtilsSkillsTool.ComponentCall call : calls) {
                        uniqueComponentNames.add(call.componentName());
                    }
                }
            } catch (RuntimeException runtimeException) {
                log.warn("Failed to analyze scripts for skill {}: {}", skillId, runtimeException.getMessage());
            }
        }

        List<ComponentConnection> connections = new ArrayList<>();

        for (String componentName : uniqueComponentNames) {
            try {
                ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                    componentName, null);

                connections.add(ComponentConnection.of(
                    workflowNodeName, componentName,
                    componentDefinition.getVersion(), componentDefinition.isConnectionRequired()));
            } catch (RuntimeException runtimeException) {
                log.debug(
                    "Component '{}' referenced in skill scripts not found in registry, skipping", componentName);
            }
        }

        return connections;
    }
}
