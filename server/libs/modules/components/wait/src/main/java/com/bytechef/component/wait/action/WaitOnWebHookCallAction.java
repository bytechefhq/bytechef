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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.wait.constant.WaitConstants.AMOUNT;
import static com.bytechef.component.wait.constant.WaitConstants.CSRF_TOKEN;
import static com.bytechef.component.wait.constant.WaitConstants.SERVICE_URL;
import static com.bytechef.component.wait.constant.WaitConstants.UNIT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

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
                string(CSRF_TOKEN)
                    .label("CSRF Token")
                    .description(
                        "Security token that must match the X-Csrf-Token HTTP header value passed by the " +
                            "caller to resume the workflow.")
                    .required(true),
                string(SERVICE_URL)
                    .label("Service URL")
                    .description(
                        "The URL of the external service to notify with the webhook resume URL when the " +
                            "workflow is suspended.")
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
            .suspendPerform(waitOnWebHookCallAction::suspendPerform)
            .beforeSuspend(waitOnWebHookCallAction::beforeSuspend)
            .resumePerform(waitOnWebHookCallAction::resumePerform);
    }

    protected Suspend suspendPerform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String csrfToken = inputParameters.getRequiredString(CSRF_TOKEN);
        int amount = inputParameters.getRequiredInteger(AMOUNT);
        String unit = inputParameters.getRequiredString(UNIT);

        ChronoUnit chronoUnit = ChronoUnit.valueOf(unit);

        Instant expiresAt = Instant.now()
            .plus(amount, chronoUnit);

        return new Suspend(
            Map.of("csrfToken", csrfToken, "expiresAt", expiresAt.toEpochMilli(), "amount", amount, "unit", unit),
            expiresAt);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    protected void beforeSuspend(
        String resumeUrl, Instant expiresAt, Parameters continueParameters, ActionContext context) {

        // When resumeUrl is available (currently null from the platform), call the external
        // service URL to notify it of the webhook resume URL
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    protected Object resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters,
        ActionContext context) {

        return Map.of("resumed", true);
    }
}
