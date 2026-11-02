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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Spring AI {@link ToolCallback} that fetches the real, dynamic options for a CLUSTER ELEMENT's property. Cluster
 * element twin of {@link LookupActionPropertyOptionsToolCallback}.
 *
 * <p>
 * The call gates in order: an unknown cluster element yields {@code cluster_element_not_found}; an unknown property
 * yields {@code property_not_found}; a property with no dynamic options yields {@code no_options_for_property}; an
 * unsatisfied {@code lookupDependsOn} sibling yields {@code dependency_missing}; a connection-bearing component with no
 * {@code connectionId} yields {@code connection_required}. On success the option list is capped at
 * {@value #MAX_OPTIONS} with a {@code truncated} flag so the chat client never tries to render hundreds of buttons.
 * </p>
 *
 * @author Ivica Cardic
 */
public class LookupClusterElementPropertyOptionsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "lookupClusterElementPropertyOptions";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(LookupClusterElementPropertyOptionsToolCallback.class);

    private static final String DESCRIPTION = """
        Fetch the real selectable options for a CLUSTER ELEMENT property whose descriptor shows "lookupRequired": true,
        e.g. knowledgeBaseId on the knowledgeBase vectorStore element. Pass the element's componentName and its
        clusterElementName (the part after the version in its type "<componentName>/v<version>/<clusterElementName>").
        First satisfy any "lookupDependsOn" siblings by placing their values in inputParameters, and pass connectionId
        when the component needs a connection. Returns componentName, clusterElementName, propertyName, an options
        array of {label, value}, and a truncated flag (true when the list was capped). Error envelopes:
        cluster_element_not_found, property_not_found, no_options_for_property, dependency_missing,
        connection_required.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "clusterElementName": {"type": "string", "description": "e.g. vectorStore, tools"},
                "propertyName": {"type": "string", "description": "Dotted paths supported: parent.child, items[].id"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string", "description": "Optional filter passed to the options provider"}
            },
            "required": ["componentName", "clusterElementName", "propertyName"]
        }""";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;
    private final ScopedConnectionGuard scopedConnectionGuard;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LookupClusterElementPropertyOptionsToolCallback(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade, PropertyOptionsResolver resolver,
        ToolStateVisibilityMetrics metrics,
        ScopedConnectionGuard scopedConnectionGuard) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.resolver = resolver;
        this.metrics = metrics;
        this.scopedConnectionGuard = scopedConnectionGuard;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            LookupClusterElementPropertyOptionsInput input = JsonUtils.read(
                toolInput, LookupClusterElementPropertyOptionsInput.class);

            String componentName = input.componentName();

            String notEntitledError = scopedConnectionGuard.toolErrorIfNotEntitled(
                componentName, input.connectionId(), toolContext);

            if (notEntitledError != null) {
                return notEntitledError;
            }

            String clusterElementName = input.clusterElementName();
            String propertyName = input.propertyName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (!AllowedComponentNamesUtils.isAllowed(componentName, toolContext)) {
                return toolError(AllowedComponentNamesUtils.getNotAllowedMessage(List.of(componentName)));
            }

            if (clusterElementName == null || clusterElementName.isBlank()) {
                return toolError("clusterElementName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null) {
                return toolError("Invocation context unavailable.");
            }

            Integer inputComponentVersion = input.componentVersion();

            int componentVersion = inputComponentVersion == null ? 1 : inputComponentVersion;

            PropertyOptionsResolver.OptionsLookupResult result = resolver.resolveClusterElementPropertyOptions(
                clusterElementDefinitionService, clusterElementDefinitionFacade, invocationContext, componentName,
                componentVersion, clusterElementName, propertyName, input.inputParameters(), input.connectionId(),
                input.searchText(),
                MAX_OPTIONS);

            if (result instanceof PropertyOptionsResolver.OptionsLookupResult.Failure failure) {
                metrics.recordStateVisibility(TOOL_NAME, failure.metricTag());

                return JsonUtils.write(failure.envelope());
            }

            PropertyOptionsResolver.OptionsLookupResult.Success success =
                (PropertyOptionsResolver.OptionsLookupResult.Success) result;

            List<Option> options = success.options();

            metrics.recordStateVisibility(TOOL_NAME, options.isEmpty() ? "empty" : "success");

            return JsonUtils.write(
                resolver.buildSuccessEnvelope(
                    componentName, "clusterElementName", clusterElementName, propertyName, options,
                    success.truncated()));
        } catch (JacksonException exception) {
            log.warn(
                "lookupClusterElementPropertyOptions rejected malformed tool input: {} - first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(LookupClusterElementPropertyOptionsToolCallback.class, TOOL_NAME,
                exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record LookupClusterElementPropertyOptionsInput(
        String componentName, @Nullable Integer componentVersion, String clusterElementName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {

        public LookupClusterElementPropertyOptionsInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
