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

package com.bytechef.platform.component.facade;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.usage.WorkflowLlmUsageEvent;
import com.bytechef.platform.ai.util.TokenUsageHolder;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@Service("actionDefinitionFacade")
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final @Nullable ApplicationEventPublisher applicationEventPublisher;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ActionDefinitionFacadeImpl(
        ConnectionService connectionService, ActionDefinitionService actionDefinitionService,
        @Nullable ApplicationEventPublisher applicationEventPublisher) {

        this.actionDefinitionService = actionDefinitionService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.connectionService = connectionService;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return actionDefinitionService.executeDynamicProperties(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            workflowId, componentConnection);
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return actionDefinitionService.executeOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, componentConnection);
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        Map<String, Long> connectionIds, Map<String, ?> extensions) {

        return actionDefinitionService.executeOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, getComponentConnections(connectionIds), extensions);
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds) {

        return actionDefinitionService.executeOutput(
            componentName, componentVersion, actionName, inputParameters, getComponentConnections(connectionIds));
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, Long jobPrincipalId, Long jobPrincipalWorkflowId,
        Long jobId, Long taskExecutionId, String workflowId, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, Map<String, ?> extensions, Long environmentId, PlatformType type,
        boolean editorEnvironment, Map<String, ?> continueParameters, Map<String, ?> resumeData,
        Instant suspendExpiresAt) {

        // Bracket the execution with the thread-local token-usage window so any sync LLM calls made by this action
        // (AI agent, LLM components) are attributed to this job. Nested executions (an agent's tool sub-actions on
        // the same thread) leave the outer window untouched — all tokens land on the same job either way.
        boolean outermostUsageWindow = !TokenUsageHolder.isTracking();

        if (outermostUsageWindow) {
            TokenUsageHolder.start();
        }

        long startMillis = System.currentTimeMillis();

        try {
            return actionDefinitionService.executePerform(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId,
                taskExecutionId, workflowId, inputParameters, getComponentConnections(connectionIds), extensions,
                environmentId, editorEnvironment, type, continueParameters, resumeData, suspendExpiresAt);
        } finally {
            if (outermostUsageWindow) {
                TokenUsageHolder.TokenUsage tokenUsage = TokenUsageHolder.getAndClear();

                if (applicationEventPublisher != null && jobId != null &&
                    (tokenUsage.promptTokens() > 0 || tokenUsage.completionTokens() > 0)) {

                    applicationEventPublisher.publishEvent(
                        new WorkflowLlmUsageEvent(
                            jobId, taskExecutionId, jobPrincipalId, jobPrincipalWorkflowId, workflowId, environmentId,
                            type, componentName, actionName, tokenUsage.model(), tokenUsage.promptTokens(),
                            tokenUsage.completionTokens(), System.currentTimeMillis() - startMillis));
                }
            }
        }
    }

    @Override
    public Object executeDryRunPerform(String componentName, int componentVersion, String actionName) {
        ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
            componentName, componentVersion, actionName);

        return DryRunOutputResolver.resolve(
            actionDefinition.isOutputDefined() ? actionDefinition.getOutputResponse() : null);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            validateConnectionActive(connection);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }

    private void validateConnectionActive(Connection connection) {
        ConnectionStatus status = connection.getStatus();

        if (status != ConnectionStatus.ACTIVE) {
            throw new ConfigurationException(
                "Connection '%s' has status %s and cannot be used for execution.".formatted(
                    connection.getName(), status),
                ConnectionErrorType.CONNECTION_NOT_ACTIVE);
        }
    }

    private Map<String, ComponentConnection> getComponentConnections(Map<String, Long> connectionIds) {
        return connectionIds.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> getComponentConnection(entry.getValue())));
    }
}
