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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftSharePointReplaceFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replaceFile")
        .title("Replace File")
        .description("Replace file in Microsoft SharePoint folder. You can replace two files that are of same type.")
        .properties(
            SITE_ID_PROPERTY,
            string(FILE_ID)
                .label("File ID")
                .description("File that will be replaced.")
                .optionsLookupDependsOn(SITE_ID)
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getFileIdOptions)
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File to replace old file.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        dateTime("createdDateTime")
                            .description("The date and time when the file was created."),
                        string("eTag")
                            .description("eTag for the entire item (metadata + content)."),
                        string(ID)
                            .description("ID of the file."),
                        dateTime("lastModifiedDateTime")
                            .description("The date and time when the file was last modified."),
                        string(NAME)
                            .description("Name of the file."),
                        integer("size")
                            .description("Size of the file in bytes."),
                        string("webUrl")
                            .description("URL to access the file in a web browser."),
                        object("createdBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string("email")
                                            .description("Email of the user who created the file."),
                                        string(ID)
                                            .description("ID of the user who created the file."),
                                        string("displayName")
                                            .description("Display name of the user who created the file."))),
                        object("lastModifiedBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string("email")
                                            .description("Email of the user who last modified the file."),
                                        string(ID)
                                            .description("ID of the user who last modified file."),
                                        string("displayName")
                                            .description("Display name of the user who last modified the file."))),
                        object(FILE)
                            .properties(
                                object("hashes")
                                    .description("Hashes of the file's binary content")
                                    .properties(
                                        string("quickXorHash")
                                            .description(
                                                "A proprietary hash of the file that can be used to determine if the " +
                                                    "contents of the file change.")),
                                string("mimeType")
                                    .description("The MIME type for the file..")))))
        .perform(MicrosoftSharePointReplaceFileAction::perform)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-share-point_v1#replace-file")
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointReplaceFileAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.put(
                "/sites/%s/drive/items/%s/content".formatted(
                    inputParameters.getRequiredString(SITE_ID), inputParameters.getRequiredString("fileId"))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(inputParameters.getRequiredFileEntry(FILE)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
