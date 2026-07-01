/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
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
 * Trigger twin of {@link SelectPropertyOptionToolCallback}.
 *
 * <p>
 * Spring AI {@link ToolCallback} that asks the chat client to render a searchable picker of a TRIGGER property's
 * dynamic options (e.g. Slack channels). Unlike {@link LookupTriggerPropertyOptionsToolCallback} (which returns the
 * options to the LLM), this returns a {@code select-property-option} marker carrying ALL fetched options so the client
 * renders them directly — the LLM never re-emits the list and cannot drop options. The picker submits the option's real
 * value (e.g. the channel id), not its label. Failures reuse the same envelopes as the lookup tool so the agent
 * self-corrects.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SelectTriggerPropertyOptionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "selectTriggerPropertyOption";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(SelectTriggerPropertyOptionToolCallback.class);

    private static final String DESCRIPTION = """
        Ask the user to PICK a value for a component TRIGGER property that has a dynamic option list (its descriptor
        shows "lookupRequired": true) — e.g. a Slack channel. The client renders a searchable dropdown of ALL options
        fetched from the connection; the user's pick (its real value/id) is captured as their next message. Satisfy any
        "lookupDependsOn" siblings via inputParameters and pass connectionId when required. Pass the canonical property
        name and triggerName from the descriptor, never the human label. Use this instead of hand-building
        askUserQuestion for option properties. On a wrong name the tool returns trigger_not_found / property_not_found
        with the valid names — retry with one of those.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "triggerName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Canonical property name (not the label)"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string"}
            },
            "required": ["componentName", "triggerName", "propertyName"]
        }""";

    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SelectTriggerPropertyOptionToolCallback(
        TriggerDefinitionService triggerDefinitionService, TriggerDefinitionFacade triggerDefinitionFacade,
        PropertyOptionsResolver resolver, ToolStateVisibilityMetrics metrics) {

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
            SelectTriggerPropertyOptionInput input = JsonUtils.read(toolInput, SelectTriggerPropertyOptionInput.class);

            String componentName = input.componentName();
            String triggerName = input.triggerName();
            String propertyName = input.propertyName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            if (triggerName == null || triggerName.isBlank()) {
                return toolError("triggerName is required and must not be blank");
            }

            if (propertyName == null || propertyName.isBlank()) {
                return toolError("propertyName is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub.");
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            PropertyOptionsResolver.OptionsLookupResult result = resolver.resolveTriggerPropertyOptions(
                triggerDefinitionService, triggerDefinitionFacade, invocationContext, componentName, componentVersion,
                triggerName, propertyName, input.inputParameters(), input.connectionId(), input.searchText(),
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
                "selectTriggerPropertyOption rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(SelectTriggerPropertyOptionToolCallback.class, TOOL_NAME, exception);
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
    public record SelectTriggerPropertyOptionInput(
        String componentName, @Nullable Integer componentVersion, String triggerName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {

        public SelectTriggerPropertyOptionInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
