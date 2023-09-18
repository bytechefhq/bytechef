
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.webhook.trigger;

import com.bytechef.component.webhook.util.WebhookUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.any;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class WebhookAutoRespondWithHTTP200Trigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("autoRespondWithHTTP200")
        .title("Auto Respond with HTTP 200 status")
        .description(
            "The webhook trigger always replies immediately with an HTTP 200 status code in response to any incoming webhook request. This guarantees execution of the webhook trigger, but does not involve any validation of the received request.")
        .type(TriggerType.STATIC_WEBHOOK)
        .outputSchema(getOutputSchemaFunction())
        .staticWebhookRequest(WebhookUtils.getStaticWebhookRequestFunction());

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connectionParameters) -> object()
            .properties(
                string(METHOD),
                object(HEADERS),
                object(PARAMETERS),
                any(BODY));
    }
}
