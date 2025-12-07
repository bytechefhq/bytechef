/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.component.webhook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.STATUS_CODE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WebhookResponseToWebhookRequestAction {

    private static final String RESPONSE_TYPE = "responseType";

    private enum ResponseType {

        JSON, RAW, BINARY, REDIRECT, NO_DATA
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("responseToWebhookRequest")
        .title("Response to Webhook Request")
        .description("Converts the response to the webhook request.")
        .properties(
            string(RESPONSE_TYPE)
                .label("Response Type")
                .description("The type of the response.")
                .options(
                    option("JSON", ResponseType.JSON.name()),
                    option("Raw", ResponseType.RAW.name()),
                    option("Binary", ResponseType.BINARY.name()),
                    option("Redirect", ResponseType.REDIRECT.name()),
                    option("No Data", ResponseType.NO_DATA.name()))
                .defaultValue(ResponseType.JSON.name()),
            object(HEADERS)
                .label("Headers")
                .description("The headers of the response.")
                .additionalProperties(string())
                .placeholder("Add header"),
            object(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == '%s'".formatted(ResponseType.JSON.name()))
                .required(true)
                .placeholder("Add property"),
            string(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == '%s'".formatted(ResponseType.RAW.name()))
                .required(true)
                .placeholder("Add property"),
            string(BODY)
                .label("Redirect URL")
                .description("The redirect URL.")
                .displayCondition("responseType == '%s'".formatted(ResponseType.REDIRECT.name()))
                .required(true),
            fileEntry(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == '%s'".formatted(ResponseType.BINARY.name()))
                .required(true)
                .placeholder("Add property"),
            integer(STATUS_CODE)
                .label("Status Code")
                .description("The status code of the response.")
                .defaultValue(200)
                .displayCondition("responseType != '%s'".formatted(ResponseType.NO_DATA.name())))
        .output(WebhookResponseToWebhookRequestAction::output)
        .perform(WebhookResponseToWebhookRequestAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(
            Map.of(
                BODY, inputParameters.getMap(BODY, new TypeReference<>() {}, Map.of()),
                HEADERS, inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                STATUS_CODE, inputParameters.getInteger(STATUS_CODE, 200)));
    }

    protected static WebhookResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return switch (inputParameters.get(RESPONSE_TYPE, ResponseType.class)) {
            case JSON -> WebhookResponse.json(
                inputParameters.getRequiredMap(BODY, new TypeReference<>() {}),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case RAW -> WebhookResponse.raw(
                inputParameters.getRequiredString(BODY),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case BINARY -> WebhookResponse.binary(
                inputParameters.getRequiredFileEntry(BODY),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case REDIRECT -> WebhookResponse.redirect(inputParameters.getRequiredString(BODY));
            default -> WebhookResponse.noData(
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
        };
    }
}
