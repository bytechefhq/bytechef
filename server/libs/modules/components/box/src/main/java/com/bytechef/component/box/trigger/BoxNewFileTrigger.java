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
import static com.bytechef.component.box.constant.BoxConstants.NEW_FILE;
import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
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
 * @author Monika Domiter
 */
public class BoxNewFileTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_FILE)
        .title("New File")
        .description("Triggers when file is uploaded to folder.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(FOLDER_ID)
                .label("Folder")
                .description("Folder in which file uploads will trigger this webhook.")
                .options((TriggerOptionsFunction<String>) BoxUtils::getRootFolderOptions)
                .required(true))
        .outputSchema(FILE_OUTPUT_PROPERTY)
        .webhookEnable(BoxNewFileTrigger::webhookEnable)
        .webhookDisable(BoxNewFileTrigger::webhookDisable)
        .webhookRequest(BoxNewFileTrigger::webhookRequest);

    private BoxNewFileTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID,
                BoxUtils.subscribeWebhook(webhookUrl, context, FOLDER, "FILE.UPLOADED",
                    inputParameters.getRequiredString(FOLDER_ID))),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        BoxUtils.unsubscribeWebhook(outputParameters, context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return body.getContent(new TypeReference<Map<String, ?>>() {})
            .get("source");
    }
}
