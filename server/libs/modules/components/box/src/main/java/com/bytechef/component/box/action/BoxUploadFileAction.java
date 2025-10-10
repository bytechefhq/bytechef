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

package com.bytechef.component.box.action;

import static com.bytechef.component.box.constant.BoxConstants.FILE;
import static com.bytechef.component.box.constant.BoxConstants.FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NAME;
import static com.bytechef.component.box.constant.BoxConstants.PARENT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class BoxUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Uploads a small file to Box.")
        .properties(
            string(ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the folder where the file should be uploaded; if no folder is selected, the file will be " +
                        "uploaded in the root folder.")
                .options((OptionsFunction<String>) BoxUtils::getRootFolderOptions)
                .defaultValue("0")
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("entries")
                            .description("A list of files that were uploaded.")
                            .items(FILE_OUTPUT_PROPERTY))))
        .perform(BoxUploadFileAction::perform);

    private BoxUploadFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        return context
            .http(http -> http.post("https://upload.box.com/api/2.0/files/content"))
            .configuration(Http.responseType(ResponseType.JSON))
            .body(
                Http.Body.of(
                    Map.of(
                        "attributes",
                        context.json(json -> json.write(
                            Map.of(
                                NAME, fileEntry.getName(),
                                PARENT, Map.of(ID, inputParameters.getRequiredString(ID))))),
                        FILE, fileEntry),
                    BodyContentType.FORM_DATA))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
