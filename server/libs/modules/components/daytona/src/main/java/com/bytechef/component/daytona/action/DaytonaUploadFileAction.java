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

package com.bytechef.component.daytona.action;

import static com.bytechef.component.daytona.constant.DaytonaConstants.FILE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.PATH;
import static com.bytechef.component.daytona.constant.DaytonaConstants.SANDBOX_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * Uploads a file into a Daytona sandbox at a given path, so the agent can supply data files before running code that
 * reads them.
 *
 * <p>
 * Endpoint (inferred from Daytona's toolbox file conventions):
 * {@code POST /toolbox/{sandboxId}/toolbox/files/upload?path={path}} with the file as the request body. Verify against
 * your account's API version.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DaytonaUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Uploads a file into a Daytona sandbox at the given path.")
        .properties(
            string(SANDBOX_ID)
                .label("Sandbox ID")
                .description("The ID of the sandbox to upload the file into.")
                .required(true),
            string(PATH)
                .label("Path")
                .description("The absolute destination path inside the sandbox (e.g. /home/daytona/data.csv).")
                .required(true),
            fileEntry(FILE)
                .label("File")
                .description("The file to upload.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(PATH)
                            .description("The destination path the file was uploaded to."))))
        .perform(DaytonaUploadFileAction::perform);

    private DaytonaUploadFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String sandboxId = inputParameters.getRequiredString(SANDBOX_ID);
        String path = inputParameters.getRequiredString(PATH);

        context
            .http(http -> http.post("/toolbox/" + sandboxId + "/toolbox/files/upload"))
            .queryParameters(PATH, path)
            .body(Http.Body.of(inputParameters.getRequiredFileEntry(FILE)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return Map.of(PATH, path);
    }
}
