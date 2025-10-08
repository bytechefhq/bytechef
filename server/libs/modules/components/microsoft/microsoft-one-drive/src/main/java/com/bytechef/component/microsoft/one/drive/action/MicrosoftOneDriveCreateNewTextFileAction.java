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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.MIME_TYPE;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.TEXT;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createNewTextFile")
        .title("Create New Text File")
        .description("Creates a new text file in Microsoft OneDrive.")
        .properties(
            string(NAME)
                .label("File Name")
                .description("The name of the new text file.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text content to add to file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(MIME_TYPE)
                .label("File Type")
                .description("Select file type.")
                .options(
                    option("Text", "plain/text"),
                    option("CSV", "text/csv"),
                    option("XML", "text/xml"))
                .defaultValue("plain/text")
                .required(true),
            string(PARENT_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder where the file should be created; if no folder is selected, the file will be " +
                        "created in the root folder.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(outputSchema(FILE_OUTPUT_PROPERTY))
        .perform(MicrosoftOneDriveCreateNewTextFileAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveCreateNewTextFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String mimeType = inputParameters.getRequiredString(MIME_TYPE);

        String suffix = switch (mimeType) {
            case "text/csv" -> ".csv";
            case "text/xml" -> ".xml";
            default -> ".txt";
        };

        String fileName = inputParameters.getRequiredString(NAME) + suffix;
        FileEntry fileEntry = context.file(
            file1 -> file1.storeContent(fileName, inputParameters.getRequiredString(TEXT)));

        return context
            .http(http -> http.put(
                "/me/drive/items/%s:/%s:/content".formatted(
                    getFolderId(inputParameters.getString(PARENT_ID)), fileName)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(fileEntry))
            .execute()
            .getBody();
    }
}
