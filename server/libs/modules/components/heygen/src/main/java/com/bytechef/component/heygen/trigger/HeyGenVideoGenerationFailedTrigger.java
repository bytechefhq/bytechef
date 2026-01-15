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

package com.bytechef.component.heygen.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.heygen.constant.HeyGenConstants.ID;
import static com.bytechef.component.heygen.util.HeyGenUtils.addWebhook;
import static com.bytechef.component.heygen.util.HeyGenUtils.deleteWebhook;
import static com.bytechef.component.heygen.util.HeyGenUtils.getContent;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class HeyGenVideoGenerationFailedTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("videoGenerationFailedTrigger")
        .title("Video Generation Failed")
        .description("Triggers when a video fails to generate.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("video_id"),
                        string("msg"),
                        string("callback_id"))))
        .webhookEnable(HeyGenVideoGenerationFailedTrigger::webhookEnable)
        .webhookDisable(HeyGenVideoGenerationFailedTrigger::webhookDisable)
        .webhookRequest(HeyGenVideoGenerationFailedTrigger::webhookRequest);

    private HeyGenVideoGenerationFailedTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(Map.of(ID, addWebhook("avatar_video.fail", context, webhookUrl)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        deleteWebhook(context, outputParameters.getString(ID));
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return getContent(body);
    }
}
