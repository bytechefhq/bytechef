/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.platform.component.definition.WebhookResponse;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WebhookResponseToWebhookRequestAction {

    private static final String RESPONSE_TYPE = "responseType";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("responseToWebhookRequest")
        .title("Response to Webhook Request")
        .description("Converts the response to the webhook request.")
        .properties(
            integer(RESPONSE_TYPE)
                .label("Response Type")
                .description("The type of the response.")
                .options(
                    option("JSON", 1),
                    option("Raw", 2),
                    option("Binary", 3),
                    option("Redirect", 4),
                    option("No Data", 5))
                .defaultValue(1),
            object(HEADERS)
                .label("Headers")
                .description("The headers of the response.")
                .additionalProperties(string())
                .placeholder("Add header"),
            object(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == 1")
                .required(true)
                .placeholder("Add property"),
            string(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == 2")
                .required(true)
                .placeholder("Add property"),
            string(BODY)
                .label("Redirect URL")
                .description("The redirect URL.")
                .displayCondition("responseType == 4")
                .required(true),
            fileEntry(BODY)
                .label("Body")
                .description("The body of the response.")
                .displayCondition("responseType == 3")
                .required(true)
                .placeholder("Add property"),
            integer(STATUS_CODE)
                .label("Status Code")
                .description("The status code of the response.")
                .defaultValue(200)
                .displayCondition("responseType != 4"))
        .output(WebhookResponseToWebhookRequestAction::output)
        .perform(WebhookResponseToWebhookRequestAction::perform);

    protected static BaseOutputDefinition.OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return new BaseOutputDefinition.OutputResponse(
            Map.of(
                BODY, inputParameters.getMap(BODY, new TypeReference<>() {}, Map.of()),
                HEADERS, inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                STATUS_CODE, inputParameters.getInteger(STATUS_CODE, 200)));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return switch (inputParameters.getInteger(RESPONSE_TYPE)) {
            case 1 -> WebhookResponse.json(
                inputParameters.getRequiredMap(BODY, new TypeReference<>() {}),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case 2 -> WebhookResponse.raw(
                inputParameters.getRequiredString(BODY),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case 3 -> WebhookResponse.binary(
                inputParameters.getRequiredFileEntry(BODY),
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
            case 4 -> WebhookResponse.redirect(inputParameters.getRequiredString(BODY));
            default -> WebhookResponse.noData(
                inputParameters.getMap(HEADERS, new TypeReference<>() {}, Map.of()),
                inputParameters.getInteger(STATUS_CODE, 200));
        };
    }
}
