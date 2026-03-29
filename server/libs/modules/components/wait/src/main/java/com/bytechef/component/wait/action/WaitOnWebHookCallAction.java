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

package com.bytechef.component.wait.action;

import static com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse.DATA;
import static com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse.RESUMED;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.component.wait.constant.WaitConstants.AMOUNT;
import static com.bytechef.component.wait.constant.WaitConstants.SERVICE_URL;
import static com.bytechef.component.wait.constant.WaitConstants.UNIT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class WaitOnWebHookCallAction {

    public static ModifiableActionDefinition of() {
        WaitOnWebHookCallAction waitOnWebHookCallAction = new WaitOnWebHookCallAction();

        return action("onWebhookCall")
            .title("On Webhook Call")
            .description(
                "Suspends the workflow execution until a webhook call is received. An external service can " +
                    "resume the workflow by calling the webhook URL.")
            .properties(
                string(SERVICE_URL)
                    .label("Service URL")
                    .description(
                        "The URL of the external service to notify with the webhook resume URL when the " +
                            "workflow is suspended.")
                    .required(true),
                string(DATA)
                    .label("Data Schema")
                    .description(
                        "JSON schema defining the structure of the body submitted by the external service when " +
                            "resuming the workflow via the webhook URL.")
                    .controlType(JSON_SCHEMA_BUILDER)
                    .required(false),
                integer(AMOUNT)
                    .label("Expires After Amount")
                    .description("The amount of time to wait for the webhook call before the workflow times out.")
                    .required(true)
                    .defaultValue(30),
                string(UNIT)
                    .label("Expires After Unit")
                    .description("The unit of time for the expiration.")
                    .required(true)
                    .defaultValue("DAYS")
                    .options(
                        option("Seconds", "SECONDS"),
                        option("Minutes", "MINUTES"),
                        option("Hours", "HOURS"),
                        option("Days", "DAYS")))
            .output(waitOnWebHookCallAction::output)
            .perform(waitOnWebHookCallAction::perform)
            .beforeSuspend(waitOnWebHookCallAction::beforeSuspend)
            .resumePerform(waitOnWebHookCallAction::resumePerform);
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        ValueProperty<?> outputSchema = object()
            .properties(
                Objects.requireNonNullElseGet(
                    context.outputSchema(
                        curOutputSchema -> curOutputSchema.getOutputSchema(DATA, inputParameters.getString(DATA))),
                    () -> object(DATA)),
                bool(RESUMED)
                    .description("Whether the workflow was resumed by a webhook call."));

        return OutputResponse.of(
            outputSchema,
            context.outputSchema(curOutputSchema -> curOutputSchema.getSampleOutput(outputSchema)));
    }

    protected Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String serviceUrl = inputParameters.getString(SERVICE_URL);
        int amount = inputParameters.getRequiredInteger(AMOUNT);
        String unit = inputParameters.getRequiredString(UNIT);

        ChronoUnit chronoUnit = ChronoUnit.valueOf(unit);

        Instant now = Instant.now();

        Instant expiresAt = now.plus(amount, chronoUnit);

        Map<String, Object> continueParameters = new HashMap<>(
            Map.of(
                "expiresAt", expiresAt.toEpochMilli(),
                AMOUNT, amount,
                UNIT, unit));

        if (serviceUrl != null) {
            continueParameters.put(SERVICE_URL, serviceUrl);
        }

        context.suspend(new Suspend(continueParameters, expiresAt));

        return null;
    }

    protected void beforeSuspend(
        String resumeUrl, Instant expiresAt, Parameters continueParameters, ActionContext context) {

        String serviceUrl = continueParameters.getString(SERVICE_URL);

        if (serviceUrl != null && resumeUrl != null) {
            context.http(
                http -> http.post(serviceUrl)
                    .body(Body.of(Map.of("resumeUrl", resumeUrl)))
                    .configuration(Http.disableAuthorization(true))
                    .execute());
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    protected ResumeResponse resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters, Parameters data,
        ActionContext context) {

        return ResumeResponse.of(new HashMap<>(data.toMap()));
    }
}
