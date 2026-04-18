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

package com.bytechef.component.ai.agent.guardrails.checkforviolations.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.BLOCKED_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CUSTOMIZE_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_BLOCKED_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SYSTEM_MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.advisor.CheckForViolationsAdvisor;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Cluster element that assembles a {@link CheckForViolationsAdvisor} from its configured child guardrail cluster
 * elements.
 *
 * @author Ivica Cardic
 */
@Component("checkForViolations_v1_ClusterElement")
public final class CheckForViolations {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public CheckForViolations(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public ClusterElementDefinition<GuardrailsFunction> of() {
        return ComponentDsl.<GuardrailsFunction>clusterElement("checkForViolations")
            .title("Check for Violations")
            .description("Runs configured guardrail checks on the user prompt.")
            .type(GUARDRAILS)
            .properties(
                bool(CUSTOMIZE_SYSTEM_MESSAGE)
                    .label("Customize System Message")
                    .defaultValue(false),
                string(SYSTEM_MESSAGE)
                    .label("System Message")
                    .defaultValue(DEFAULT_SYSTEM_MESSAGE)
                    .displayCondition(CUSTOMIZE_SYSTEM_MESSAGE + " == true"),
                string(BLOCKED_MESSAGE)
                    .label("Blocked Message")
                    .defaultValue(DEFAULT_BLOCKED_MESSAGE))
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Advisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        CheckForViolationsAdvisor.Builder builder = CheckForViolationsAdvisor.builder()
            .blockedMessage(inputParameters.getString(BLOCKED_MESSAGE, DEFAULT_BLOCKED_MESSAGE));

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        List<ClusterElement> checkChildren = clusterElementMap.getClusterElements(CHECK_FOR_VIOLATIONS);

        // Resolve each child's GuardrailCheckFunction exactly once and reuse the instance for both the LLM-stage
        // detection (hasLlmChild) and the builder.add loop below. The previous implementation called
        // clusterElementDefinitionService.getClusterElement twice per child — a Spring-registered-bean lookup each
        // time. With a 10-child cluster the second pass halved agent-invocation lookup cost for no functional
        // benefit.
        List<ResolvedCheckChild> resolvedChildren = resolveChildren(checkChildren);

        // Only resolve the shared chat client when at least one check declares GuardrailStage.LLM — otherwise
        // we'd pay the modelFunction.apply(...) cost (which typically materialises provider configuration and can
        // validate API keys) on every agent invocation even when no LLM guardrail needs it. The absence of a MODEL
        // child when none is needed is not an error; the absence when at least one LLM child exists still throws
        // MissingModelChildException below.
        boolean needsLlm = hasLlmChild(resolvedChildren);
        ChatClient sharedChatClient = needsLlm
            ? resolveSharedChatClient(clusterElementMap, componentConnections)
            : null;

        if (sharedChatClient == null && needsLlm) {
            throw new MissingModelChildException("checkForViolations");
        }

        for (ResolvedCheckChild resolved : resolvedChildren) {
            ClusterElement element = resolved.element();
            ComponentConnection connection = componentConnections.get(element.getWorkflowNodeName());

            builder.add(
                element.getClusterElementName(),
                resolved.function(),
                ParametersFactory.create(element.getParameters()),
                ParametersFactory.create(connection == null ? Map.of() : connection.getParameters()),
                inputParameters,
                ParametersFactory.create(element.getExtensions()),
                componentConnections,
                sharedChatClient);
        }

        return builder.build();
    }

    private List<ResolvedCheckChild> resolveChildren(List<ClusterElement> children) {
        List<ResolvedCheckChild> resolved = new ArrayList<>(children.size());

        for (ClusterElement element : children) {
            GuardrailCheckFunction function = clusterElementDefinitionService.getClusterElement(
                element.getComponentName(), element.getComponentVersion(),
                element.getClusterElementName());

            resolved.add(new ResolvedCheckChild(element, function));
        }

        return resolved;
    }

    private ChatClient resolveSharedChatClient(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> componentConnections) throws Exception {

        Optional<ClusterElement> modelElement = clusterElementMap.fetchClusterElement(MODEL);

        if (modelElement.isEmpty()) {
            return null;
        }

        ClusterElement element = modelElement.get();

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            element.getComponentName(), element.getComponentVersion(), element.getClusterElementName());

        ComponentConnection connection = componentConnections.get(element.getWorkflowNodeName());

        Object model = modelFunction.apply(
            ParametersFactory.create(element.getParameters()),
            ParametersFactory.create(connection == null ? Map.of() : connection.getParameters()),
            false);

        if (!(model instanceof ChatModel chatModel)) {
            throw new IllegalArgumentException(
                "MODEL child '" + element.getClusterElementName() + "' on component '" + element.getComponentName()
                    + "' v" + element.getComponentVersion() + " returned "
                    + (model == null ? "null" : model.getClass()
                        .getName())
                    + "; CheckForViolations requires a ChatModel. Attach a chat-capable model.");
        }

        return ChatClient.create(chatModel);
    }

    /**
     * Delegates to the canonical {@link GuardrailCheckFunction#stage()} signal rather than a hardcoded name list so new
     * LLM-based guardrails added later are picked up automatically — no need to touch this method when adding a cluster
     * element that declares {@link GuardrailStage#LLM}. Operates on already-resolved functions so we do not repeat the
     * per-child lookup.
     */
    private static boolean hasLlmChild(List<ResolvedCheckChild> resolvedChildren) {
        for (ResolvedCheckChild resolved : resolvedChildren) {
            if (resolved.function()
                .stage() == GuardrailStage.LLM) {
                return true;
            }
        }

        return false;
    }

    private record ResolvedCheckChild(ClusterElement element, GuardrailCheckFunction function) {
    }
}
