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

package com.bytechef.automation.configuration.facade;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class AiWorkflowGeneratorFacadeImpl implements AiWorkflowGeneratorFacade {

    private static final int MAX_LABEL_LENGTH = 60;

    private final ObjectProvider<CopilotWorkflowGenerator> copilotWorkflowGeneratorProvider;
    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AiWorkflowGeneratorFacadeImpl(
        ObjectProvider<CopilotWorkflowGenerator> copilotWorkflowGeneratorProvider,
        ProjectWorkflowFacade projectWorkflowFacade) {

        this.copilotWorkflowGeneratorProvider = copilotWorkflowGeneratorProvider;
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    /**
     * Intentionally NOT {@code @Transactional}: {@code copilotWorkflowGenerator.generateWorkflow} drives the autonomous
     * agent synchronously (up to ~10 minutes), so it must not hold a database transaction open. The empty workflow is
     * persisted first via {@link ProjectWorkflowFacade#addWorkflow} — a separate transaction that commits through the
     * facade proxy before generation starts — so the agent can read and update the row it is filling.
     */
    @Override
    public ProjectWorkflow generateWorkflow(long projectId, String prompt) {
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank");
        }

        CopilotWorkflowGenerator copilotWorkflowGenerator = copilotWorkflowGeneratorProvider.getIfAvailable();

        if (copilotWorkflowGenerator == null) {
            throw new IllegalStateException(
                "AI Copilot is not enabled. Set bytechef.ai.copilot.enabled=true to use workflow generation.");
        }

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, buildEmptyDefinition(prompt));

        // Design-time generation carries no request-supplied environment; resolve the AI provider under the ambient
        // EnvironmentContext (bound by the request boundary, PRODUCTION when unset) rather than defaulting silently
        // inside the agent.
        int environmentId = EnvironmentContext.getCurrentEnvironment()
            .ordinal();

        copilotWorkflowGenerator.generateWorkflow(
            projectWorkflow.getWorkflowId(), prompt.strip(), null, Set.of(), environmentId);

        return projectWorkflow;
    }

    private static String buildEmptyDefinition(String prompt) {
        String label = prompt.strip();

        if (label.length() > MAX_LABEL_LENGTH) {
            label = label.substring(0, MAX_LABEL_LENGTH)
                .strip();
        }

        return JsonUtils.write(
            Map.of(
                "label", label,
                "description", "",
                "inputs", List.of(),
                "triggers", List.of(
                    Map.of("label", "Manual", "name", "trigger_1", "type", "manual/v1/manual")),
                "tasks", List.of()));
    }
}
