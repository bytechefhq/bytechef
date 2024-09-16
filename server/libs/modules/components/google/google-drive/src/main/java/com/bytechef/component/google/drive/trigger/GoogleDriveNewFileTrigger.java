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

package com.bytechef.component.google.drive.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.GOOGLE_FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;
import static com.bytechef.component.google.drive.util.GoogleDriveUtils.getPollOutput;

import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;

/**
 * @author Monika Kušter
 */
public class GoogleDriveNewFileTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFile")
        .title("New File")
        .description("Triggers when new file is uploaded to Google Drive.")
        .type(TriggerType.POLLING)
        .properties(
            string(PARENT_FOLDER)
                .label("Parent folder")
                .options((TriggerOptionsFunction<String>) GoogleDriveUtils::getFolderOptions)
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(GOOGLE_FILE_OUTPUT_PROPERTY)))
        .poll(GoogleDriveNewFileTrigger::poll);

    private GoogleDriveNewFileTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        return getPollOutput(inputParameters, connectionParameters, closureParameters, true);
    }
}
