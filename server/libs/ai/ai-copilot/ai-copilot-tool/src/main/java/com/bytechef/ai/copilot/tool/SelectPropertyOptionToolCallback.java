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
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * Spring AI {@link ToolCallback} that asks the chat client to render a searchable picker of an ACTION property's
 * dynamic options (e.g. Slack channels). Unlike {@link LookupActionPropertyOptionsToolCallback} (which returns the
 * options to the LLM), this returns a {@code select-property-option} marker carrying ALL fetched options so the client
 * renders them directly — the LLM never re-emits the list and cannot drop options. The picker submits the option's real
 * value (e.g. the channel id), not its label. Failures reuse the same envelopes as the lookup tool so the agent
 * self-corrects.
 *
 * @author Ivica Cardic
 */
public class SelectPropertyOptionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "selectPropertyOption";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(SelectPropertyOptionToolCallback.class);

    private static final String DESCRIPTION = """
        Ask the user to PICK a value for a component ACTION property that has a dynamic option list (its descriptor
        shows "lookupRequired": true) — e.g. a Slack channel. The client renders a searchable dropdown of ALL options
        fetched from the connection; the user's pick (its real value/id) is captured as their next message. Satisfy any
        "lookupDependsOn" siblings via inputParameters and pass connectionId when required. Pass the canonical property
        name and actionName from the descriptor, never the human label. Use this instead of hand-building
        askUserQuestion for option properties. On a wrong name the tool returns action_not_found / property_not_found
        with the valid names — retry with one of those.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "actionName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Canonical property name (not the label)"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string"}
            },
            "required": ["componentName", "actionName", "propertyName"]
        }""";

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SelectPropertyOptionToolCallback(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        PropertyOptionsResolver resolver, ToolStateVisibilityMetrics metrics) {

        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.resolver = resolver;
        this.metrics = metrics;
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
            SelectPropertyOptionInput input = JsonUtils.read(toolInput, SelectPropertyOptionInput.class);

            String componentName = input.componentName();
            String actionName = input.actionName();
            String propertyName = input.propertyName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (actionName == null || actionName.isBlank()) {
                return toolError("actionName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null) {
                return toolError("Invocation context unavailable.");
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            PropertyOptionsResolver.OptionsLookupResult result = resolver.resolveActionPropertyOptions(
                actionDefinitionService, actionDefinitionFacade, invocationContext, componentName, componentVersion,
                actionName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(),
                MAX_OPTIONS);

            if (result instanceof PropertyOptionsResolver.OptionsLookupResult.Failure failure) {
                metrics.recordStateVisibility(TOOL_NAME, failure.metricTag());

                return JsonUtils.write(failure.envelope());
            }

            PropertyOptionsResolver.OptionsLookupResult.Success success =
                (PropertyOptionsResolver.OptionsLookupResult.Success) result;

            List<Option> options = success.options();

            metrics.recordStateVisibility(TOOL_NAME, options.isEmpty() ? "empty" : "success");

            return buildMarker(componentName, propertyName, options, success.truncated());
        } catch (JacksonException exception) {
            log.warn(
                "selectPropertyOption rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(SelectPropertyOptionToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String buildMarker(String componentName, String propertyName, List<Option> options, boolean truncated) {
        List<Map<String, Object>> optionRows = new ArrayList<>(options.size());

        for (Option option : options) {
            Map<String, Object> optionRow = new LinkedHashMap<>();

            optionRow.put("label", option.getLabel());
            optionRow.put("value", option.getValue());

            optionRows.add(optionRow);
        }

        Map<String, Object> marker = new LinkedHashMap<>();

        marker.put("kind", "select-property-option");
        marker.put("componentName", componentName);
        marker.put("propertyName", propertyName);
        marker.put("options", optionRows);
        marker.put("truncated", truncated);

        try {
            return JsonUtils.write(marker);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize select-property-option marker", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SelectPropertyOptionInput(
        String componentName, @Nullable Integer componentVersion, String actionName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {

        public SelectPropertyOptionInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
