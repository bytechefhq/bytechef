/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.FORBIDDEN;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INTERNAL_ERROR;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INVALID_INPUT;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.NEW_API_REQUEST;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE_TO_API_REQUEST;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.RESPONSE_TYPE;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.SUCCESS;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.WebhookResponse;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformResponseToApiRequestAction {

    public final ModifiableActionDefinition actionDefinition = action(RESPONSE_TO_API_REQUEST)
        .title("Response to API Request")
        .description("Converts the response to API request.")
        .properties(
            string(RESPONSE_TYPE)
                .label("Response Type")
                .description("The type of the response.")
                .options(
                    option("Success Response", SUCCESS),
                    option("Internal Error Response", INTERNAL_ERROR),
                    option("Invalid Input Response", INVALID_INPUT),
                    option("Forbidden Response", FORBIDDEN))
                .required(true),
            dynamicProperties(RESPONSE)
                .description("The properties of the response.")
                .propertiesLookupDependsOn(RESPONSE_TYPE)
                .properties(this::responseProperties)
                .required(true))
        .output(this::output)
        .perform(this::perform);

    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApiPlatformResponseToApiRequestAction(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return new OutputResponse(getResponse(inputParameters));
    }

    protected Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        Object response = getResponse(inputParameters);

        return switch (inputParameters.getString(RESPONSE_TYPE)) {
            case SUCCESS -> WebhookResponse.json(response);
            case INTERNAL_ERROR -> WebhookResponse.json(response, HttpStatus.BAD_REQUEST);
            case INVALID_INPUT -> WebhookResponse.json(response, HttpStatus.INTERNAL_SERVER_ERROR);
            case FORBIDDEN -> WebhookResponse.json(response, HttpStatus.FORBIDDEN);
            default ->
                throw new IllegalStateException("Unexpected value: " + inputParameters.getInteger(RESPONSE_TYPE));
        };
    }

    protected List<? extends ValueProperty<?>> responseProperties(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext actionContext) {

        String workflowId = ((ActionContextAware) actionContext).getWorkflowId();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflow)
            .stream()
            .findFirst()
            .orElse(null);

        if (workflowTrigger == null) {
            return List.of();
        }

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        if (!Objects.equals(workflowNodeType.componentOperationName(), NEW_API_REQUEST)) {
            return List.of();
        }

        Map<String, ?> parameters = workflowTrigger.getParameters();

        Map<String, ?> responseMap = MapUtils.getMap(parameters, RESPONSE, Map.of());

        return switch (inputParameters.getInteger(RESPONSE_TYPE)) {
            case 1 -> getJsonSchemaProperty(SUCCESS, responseMap, actionContext);
            case 2 -> getJsonSchemaProperty(INVALID_INPUT, responseMap, actionContext);
            case 3 -> getJsonSchemaProperty(INTERNAL_ERROR, responseMap, actionContext);
            case 4 -> getJsonSchemaProperty(FORBIDDEN, responseMap, actionContext);
            default ->
                throw new IllegalStateException("Unexpected value: " + inputParameters.getInteger(RESPONSE_TYPE));
        };
    }

    private static List<ModifiableValueProperty<?, ?>> getJsonSchemaProperty(
        String name, Map<String, ?> responseMap, ActionContext actionContext) {

        ModifiableValueProperty<?, ?> property = (ModifiableValueProperty<?, ?>) actionContext.outputSchema(
            outputSchema -> outputSchema.getOutputSchema(name, (String) responseMap.get(name)));

        if (property == null) {
            return List.of();
        }

        return List.of(property);
    }

    private static Object getResponse(Parameters inputParameters) {
        Map<String, ?> responseMap = MapUtils.getMap(inputParameters, RESPONSE, Map.of());

        String propertyName = switch (inputParameters.getInteger(RESPONSE_TYPE)) {
            case 1 -> SUCCESS;
            case 2 -> INVALID_INPUT;
            case 3 -> INTERNAL_ERROR;
            case 4 -> FORBIDDEN;
            default ->
                throw new IllegalStateException("Unexpected value: " + inputParameters.getInteger(RESPONSE_TYPE));
        };

        return responseMap.containsKey(propertyName) ? responseMap.get(propertyName) : Map.of();
    }
}
