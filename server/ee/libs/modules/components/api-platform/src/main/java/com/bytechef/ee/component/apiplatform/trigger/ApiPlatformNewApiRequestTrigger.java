/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.trigger;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.BODY;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.FORBIDDEN;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.HEADERS;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INTERNAL_ERROR;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.INVALID_INPUT;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.METHOD;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.NEW_API_REQUEST;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.PARAMETERS;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.SUCCESS;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformNewApiRequestTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger(NEW_API_REQUEST)
        .title("New API Request")
        .description(
            "It allows you to customize success and failure responses within the workflow and deliver relevant data payloads for the requester to process.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .properties(
            object(ApiPlatformConstants.REQUEST)
                .label("Request")
                .description("The schema definition for the request input.")
                .required(true)
                .expressionEnabled(false)
                .properties(
                    string(HEADERS)
                        .label("Headers")
                        .placeholder("Edit Headers schema")
                        .description(
                            "The schema definition for headers of the request. The properties of the schema can only be strings or string arrays. The field only expects custom headers, standard headers like Content-Type do not need to be specified.")
                        .controlType(JSON_SCHEMA_BUILDER),
                    string(PARAMETERS)
                        .label("Parameters")
                        .placeholder("Edit Parameters schema")
                        .description("The schema definition for parameters of the request.")
                        .controlType(JSON_SCHEMA_BUILDER),
                    string(BODY)
                        .label("Body")
                        .placeholder("Edit Body schema")
                        .description("The schema definition for body of the request.")
                        .controlType(JSON_SCHEMA_BUILDER)),
            object(ApiPlatformConstants.RESPONSE)
                .label("Response")
                .description("The schema definition for the response output.")
                .required(true)
                .expressionEnabled(false)
                .properties(
                    string(SUCCESS)
                        .label("Success")
                        .placeholder("Edit Success schema")
                        .description("The schema definition for a successful response.")
                        .controlType(JSON_SCHEMA_BUILDER),
                    string(INVALID_INPUT)
                        .label("Invalid Input")
                        .placeholder("Edit Invalid Input schema")
                        .description("The schema definition for the invalid input error response.")
                        .controlType(JSON_SCHEMA_BUILDER),
                    string(INTERNAL_ERROR)
                        .label("Internal Error")
                        .placeholder("Edit Internal Error schema")
                        .description("The schema definition for the internal error response.")
                        .controlType(JSON_SCHEMA_BUILDER),
                    string(FORBIDDEN)
                        .label("Forbidden")
                        .placeholder("Edit Forbidden schema")
                        .description("The schema definition for the forbidden error response.")
                        .controlType(JSON_SCHEMA_BUILDER)),
            integer(ApiPlatformConstants.TIMEOUT)
                .label("Timeout (ms)")
                .description(
                    "The incoming request will time out after the specified number of milliseconds. The max wait time before a timeout is 5 minutes."))
        .workflowSyncExecution(true)
        .output(this::output)
        .webhookRequest(this::webhookResult);

    public ApiPlatformNewApiRequestTrigger() {
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context) {

        Map<String, ?> request = inputParameters.getMap("request", Map.of());
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(string(METHOD));

        ModifiableValueProperty<?, ?> headers = (ModifiableValueProperty<?, ?>) context.outputSchema(
            outputSchema -> outputSchema.getOutputSchema(HEADERS, (String) request.get(HEADERS)));

        if (headers != null) {
            properties.add(headers);
        }

        ModifiableValueProperty<?, ?> parameters = (ModifiableValueProperty<?, ?>) context.outputSchema(
            outputSchema -> outputSchema.getOutputSchema(PARAMETERS, (String) request.get(PARAMETERS)));

        if (parameters != null) {
            properties.add(parameters);
        }

        ModifiableValueProperty<?, ?> body = (ModifiableValueProperty<?, ?>) context.outputSchema(
            outputSchema -> outputSchema.getOutputSchema(BODY, (String) request.get(BODY)));

        if (body != null) {
            properties.add(body);
        }

        return OutputResponse.of(object().properties(properties));
    }

    protected Map<String, ?> webhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        Map<String, ?> headerMap = headers.toMap();
        Map<String, ?> parameterMap = parameters.toMap();

        return new HashMap<>() {
            {
                put(METHOD, method);
                put(HEADERS, headerMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformNewApiRequestTrigger::checkList)));
                put(PARAMETERS, parameterMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformNewApiRequestTrigger::checkList)));

                if (body != null) {
                    put(BODY, body.getContent());
                }
            }
        };
    }

    private static Object checkList(Map.Entry<String, ?> entry) {
        Object value = entry.getValue();

        if (value instanceof List<?> list && list.size() == 1) {
            value = list.getFirst();
        }

        return value;
    }
}
