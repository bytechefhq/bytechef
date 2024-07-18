/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.box.trigger;

import static com.bytechef.component.box.constant.BoxConstants.FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.box.constant.BoxConstants.FOLDER;
import static com.bytechef.component.box.constant.BoxConstants.FOLDER_ID;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NEW_FOLDER;
import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class BoxNewFolderTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_FOLDER)
        .title("New Folder")
        .description("Triggers when folder is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(FOLDER_ID)
                .label("Folder")
                .description("Folder in which new folder will trigger this webhook.")
                .options((TriggerOptionsFunction<String>) BoxUtils::getRootFolderOptions)
                .required(true))
        .outputSchema(FILE_OUTPUT_PROPERTY)
        .dynamicWebhookEnable(BoxNewFolderTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(BoxNewFolderTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(BoxNewFolderTrigger::dynamicWebhookRequest);

    private BoxNewFolderTrigger() {
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, Context context) {

        return new DynamicWebhookEnableOutput(
            Map.of(ID,
                BoxUtils.subscribeWebhook(webhookUrl, context, FOLDER, "FOLDER.CREATED",
                    inputParameters.getRequiredString(FOLDER_ID))),
            null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, Context context) {

        BoxUtils.unsubscribeWebhook(outputParameters, context);
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, ?>>() {})
            .get("source");
    }
}
