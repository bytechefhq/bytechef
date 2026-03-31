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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class DropboxDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadFile")
        .title("Download File")
        .description("Download a file from Dropbox.")
        .properties(
            string(PATH)
                .label("File Path")
                .description("The path of the file to download.")
                .exampleValue("/folder1/sourceFile.txt")
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(DropboxDownloadFileAction::perform);

    private DropboxDownloadFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String headerJson = context.json(json -> json.write(Map.of(PATH, inputParameters.getRequiredString(PATH))));

        return context.http(http -> http.post("https://content.dropboxapi.com/2/files/download"))
            .headers(Map.of("Dropbox-API-Arg", List.of(headerJson)))
            .configuration(Http.responseType(Http.ResponseType.BINARY))
            .execute()
            .getBody();
    }
}
