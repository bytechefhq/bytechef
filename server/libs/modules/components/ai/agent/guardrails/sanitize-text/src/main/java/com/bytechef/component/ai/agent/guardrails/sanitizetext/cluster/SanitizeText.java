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

package com.bytechef.component.ai.agent.guardrails.sanitizetext.cluster;

import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.advisor.SanitizeTextAdvisor;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
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
 * Cluster element that assembles a {@link SanitizeTextAdvisor} from its configured child sanitizer cluster elements.
 *
 * @author Ivica Cardic
 */
@Component("sanitizeText_v1_ClusterElement")
public final class SanitizeText {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public SanitizeText(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public ClusterElementDefinition<GuardrailsFunction> of() {
        return ComponentDsl.<GuardrailsFunction>clusterElement("sanitizeText")
            .title("Sanitize Text")
            .description("Runs configured sanitizers over the assistant response.")
            .type(GUARDRAILS)
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Advisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        SanitizeTextAdvisor.Builder builder = SanitizeTextAdvisor.builder();

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        List<ClusterElement> sanitizerChildren = clusterElementMap.getClusterElements(SANITIZE_TEXT);

        // Resolve each child's GuardrailSanitizerFunction exactly once and reuse it for both the LLM-stage detection
        // (hasLlmSanitizer) and the builder.add loop below. Parallel to CheckForViolations.resolveChildren: the
        // previous implementation called clusterElementDefinitionService.getClusterElement twice per child — a
        // Spring-registered-bean lookup each time.
        List<ResolvedSanitizerChild> resolvedChildren = resolveChildren(sanitizerChildren);

        // Only resolve the shared chat client when at least one sanitizer declares GuardrailStage.LLM — see
        // CheckForViolations for the parity rationale: modelFunction.apply(...) is not free and should not run
        // when no LLM sanitizer consumes the result.
        boolean needsLlm = hasLlmSanitizer(resolvedChildren);
        ChatClient sharedChatClient = needsLlm
            ? resolveSharedChatClient(clusterElementMap, componentConnections)
            : null;

        if (sharedChatClient == null && needsLlm) {
            throw new MissingModelChildException("SanitizeText");
        }

        for (ResolvedSanitizerChild resolved : resolvedChildren) {
            ClusterElement element = resolved.element();
            ComponentConnection connection = componentConnections.get(element.getWorkflowNodeName());

            builder.add(
                element.getClusterElementName(),
                resolved.function(),
                ParametersFactory.create(element.getParameters()),
                ParametersFactory.create(connection == null ? Map.of() : connection.getParameters()),
                ParametersFactory.create(element.getExtensions()),
                componentConnections,
                sharedChatClient);
        }

        return builder.build();
    }

    private List<ResolvedSanitizerChild> resolveChildren(List<ClusterElement> children) {
        List<ResolvedSanitizerChild> resolved = new ArrayList<>(children.size());

        for (ClusterElement element : children) {
            GuardrailSanitizerFunction function = clusterElementDefinitionService.getClusterElement(
                element.getComponentName(), element.getComponentVersion(),
                element.getClusterElementName());

            resolved.add(new ResolvedSanitizerChild(element, function));
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
                    + "; SanitizeText requires a ChatModel. Attach a chat-capable model.");
        }

        return ChatClient.create(chatModel);
    }

    /**
     * Delegates to the canonical {@link GuardrailSanitizerFunction#stage()} signal rather than a hardcoded name list so
     * new LLM-based sanitizers added later are picked up automatically — no need to touch this method when adding a
     * cluster element that declares {@link GuardrailStage#LLM}. Operates on already-resolved functions so we do not
     * repeat the per-child lookup.
     */
    private static boolean hasLlmSanitizer(List<ResolvedSanitizerChild> resolvedChildren) {
        for (ResolvedSanitizerChild resolved : resolvedChildren) {
            if (resolved.function()
                .stage() == GuardrailStage.LLM) {
                return true;
            }
        }

        return false;
    }

    private record ResolvedSanitizerChild(ClusterElement element, GuardrailSanitizerFunction function) {
    }
}
