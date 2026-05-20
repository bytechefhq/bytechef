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

package com.bytechef.component.microsoft.one.drive.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;
import static com.bytechef.microsoft.commons.MicrosoftConstants.RECURSIVE;
import static com.bytechef.microsoft.commons.MicrosoftConstants.RECURSIVE_PROPERTY;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveNewFileTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFile")
        .title("New File")
        .description("Triggers when file is uploaded to folder.")
        .type(TriggerType.POLLING)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-one-drive_v1#new-file")
        .properties(
            string(PARENT_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder to watch for new files. If no folder is specified, the root folder will be used.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false),
            RECURSIVE_PROPERTY)
        .output(outputSchema(FILE_OUTPUT_PROPERTY))
        .poll(MicrosoftOneDriveNewFileTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveNewFileTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String url = "/me/drive/items/%s/".formatted(getFolderId(inputParameters.getString(PARENT_ID)));

        return MicrosoftTriggerUtils.poll(
            inputParameters.getBoolean(RECURSIVE) ? url + "delta" : url + "children", "file", closureParameters,
            context);
    }
}
