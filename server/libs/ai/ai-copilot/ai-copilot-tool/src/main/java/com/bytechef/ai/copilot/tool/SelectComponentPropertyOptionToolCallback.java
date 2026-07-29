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
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
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
 * Unified variant of {@link SelectPropertyOptionToolCallback} / {@link SelectTriggerPropertyOptionToolCallback} for
 * agents that want ONE pinned picker tool for both operation kinds: a {@code kind} input (ACTION | TRIGGER) selects the
 * resolution path. The tool name stays {@code selectPropertyOption} and the emitted {@code select-property-option}
 * marker is unchanged, so the chat client's picker rendering works without changes. The per-kind twins remain for
 * surfaces that keep them registered.
 *
 * @author Ivica Cardic
 */
public class SelectComponentPropertyOptionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "selectPropertyOption";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(SelectComponentPropertyOptionToolCallback.class);

    private static final String DESCRIPTION = """
        Ask the user to PICK a value for a component ACTION or TRIGGER property that has a dynamic option list (its
        descriptor shows "lookupRequired": true) — e.g. a Slack channel. Supply kind (ACTION or TRIGGER) and
        operationName (the canonical action/trigger name from the descriptor, never the human label). The client
        renders a searchable dropdown of ALL options fetched from the connection; the user's pick (its real value/id)
        is captured as their next message. Satisfy any "lookupDependsOn" siblings via inputParameters and pass
        connectionId when required. Use this instead of hand-building askUserQuestion for option properties. On a
        wrong name the tool returns action_not_found / trigger_not_found / property_not_found with the valid names —
        retry with one of those.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "kind": {"type": "string", "enum": ["ACTION", "TRIGGER"], "description": "Operation kind"},
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "operationName": {"type": "string", "description": "Canonical action or trigger name (not the label)"},
                "propertyName": {"type": "string", "description": "Canonical property name (not the label)"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string"}
            },
            "required": ["kind", "componentName", "operationName", "propertyName"]
        }""";

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SelectComponentPropertyOptionToolCallback(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver resolver, ToolStateVisibilityMetrics metrics) {

        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
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
            SelectComponentPropertyOptionInput input = JsonUtils.read(
                toolInput, SelectComponentPropertyOptionInput.class);

            String kind = input.kind();
            String componentName = input.componentName();
            String operationName = input.operationName();
            String propertyName = input.propertyName();

            if (kind == null || !(kind.equals("ACTION") || kind.equals("TRIGGER"))) {
                return toolError("kind is required and must be ACTION or TRIGGER");
            }

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (operationName == null || operationName.isBlank()) {
                return toolError("operationName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null) {
                return toolError("Invocation context unavailable.");
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            PropertyOptionsResolver.OptionsLookupResult result = kind.equals("ACTION")
                ? resolver.resolveActionPropertyOptions(
                    actionDefinitionService, actionDefinitionFacade, invocationContext, componentName,
                    componentVersion, operationName, propertyName, input.inputParameters(), input.connectionId(),
                    input.searchText(), MAX_OPTIONS)
                : resolver.resolveTriggerPropertyOptions(
                    triggerDefinitionService, triggerDefinitionFacade, invocationContext, componentName,
                    componentVersion, operationName, propertyName, input.inputParameters(), input.connectionId(),
                    input.searchText(), MAX_OPTIONS);

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

            return ToolErrors.runtimeFailure(SelectComponentPropertyOptionToolCallback.class, TOOL_NAME, exception);
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

    public record SelectComponentPropertyOptionInput(
        String kind, String componentName, @Nullable Integer componentVersion, String operationName,
        String propertyName, @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId,
        @Nullable String searchText) {

        public SelectComponentPropertyOptionInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
