/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Spring AI {@link ToolCallback} that fetches the real, dynamic options for a component action's property (e.g. the
 * channels available on a Slack connection). The agent learns a property needs a lookup from the
 * {@code "lookupRequired": true} marker that {@code ToolUtils.appendLookupMetadata} writes into the action's catalog
 * descriptor; this tool is what it calls to resolve them. Returned options are meant to be surfaced through
 * {@code askUserQuestion} so the user never has to type a raw id and the agent never has to guess.
 *
 * <p>
 * The call gates in order: an unknown action yields {@code action_not_found}; an unknown property yields
 * {@code property_not_found}; a property with no dynamic options yields {@code no_options_for_property}; an unsatisfied
 * {@code lookupDependsOn} sibling yields {@code dependency_missing}; a connection-bearing component with no
 * {@code connectionId} yields {@code connection_required}. On success the option list is capped at
 * {@value #MAX_OPTIONS} with a {@code truncated} flag so the chat client never tries to render hundreds of buttons.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class LookupActionPropertyOptionsToolCallback implements ToolCallback {

    static final String TOOL_NAME = "lookupActionPropertyOptions";

    private static final int MAX_OPTIONS = 25;

    private static final Logger log = LoggerFactory.getLogger(LookupActionPropertyOptionsToolCallback.class);

    private static final String DESCRIPTION = """
        Fetch the real selectable options for a component ACTION property whose descriptor shows
        "lookupRequired": true (e.g. a Slack channel, a Google Sheets sheet). Call this before asking the user, then
        render the returned options via askUserQuestion. First satisfy any "lookupDependsOn" siblings by placing their
        values in inputParameters, and pass connectionId when the component needs a connection. Returns componentName,
        actionName, propertyName, an options array of {label, value}, and a truncated flag (true when the list was
        capped). Error envelopes: action_not_found, property_not_found, no_options_for_property, dependency_missing,
        connection_required.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"},
                "actionName": {"type": "string"},
                "propertyName": {"type": "string", "description": "Dotted paths supported: parent.child, items[].id"},
                "inputParameters": {"type": "object", "description": "Sibling values required by lookupDependsOn"},
                "connectionId": {"type": "integer"},
                "searchText": {"type": "string", "description": "Optional filter passed to the options provider"}
            },
            "required": ["componentName", "actionName", "propertyName"]
        }""";

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public LookupActionPropertyOptionsToolCallback(
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
            LookupActionPropertyOptionsInput input = JsonUtils.read(toolInput, LookupActionPropertyOptionsInput.class);

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

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub.");
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

            metrics.recordStateVisibility(TOOL_NAME, success.options()
                .isEmpty() ? "empty" : "success");

            return JsonUtils.write(
                resolver.buildSuccessEnvelope(
                    componentName, "actionName", actionName, propertyName, success.options(), success.truncated()));
        } catch (JacksonException exception) {
            log.warn(
                "lookupActionPropertyOptions rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(LookupActionPropertyOptionsToolCallback.class, TOOL_NAME, exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record LookupActionPropertyOptionsInput(
        String componentName, @Nullable Integer componentVersion, String actionName, String propertyName,
        @Nullable Map<String, Object> inputParameters, @Nullable Long connectionId, @Nullable String searchText) {

        public LookupActionPropertyOptionsInput {
            inputParameters = inputParameters == null ? null : Map.copyOf(inputParameters);
        }
    }
}
