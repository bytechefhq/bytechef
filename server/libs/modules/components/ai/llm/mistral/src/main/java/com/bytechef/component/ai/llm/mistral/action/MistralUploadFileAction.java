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

package com.bytechef.component.ai.llm.mistral.action;

import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.FILE;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.PURPOSE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MistralUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Extracts text and structured content from documents.")
        .properties(
            string(PURPOSE)
                .label("Purpose")
                .description("Model to use.")
                .options(
                    option("Fine Tune", "fine-tune"),
                    option("Batch", "batch"),
                    option("OCR", "ocr"))
                .required(false),
            fileEntry(FILE)
                .label("File")
                .description("The file to be uploaded.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("ID of the uploaded file."),
                        string("object")
                            .description("The object type, which is always file."),
                        integer("bytes")
                            .description("Size of the file in bytes."),
                        integer("created_at")
                            .description("The UNIX timestamp (in seconds) of the event."),
                        string("filename")
                            .description("Name of the uploaded file."),
                        string("purpose")
                            .description("The intended purpose of the uploaded file."),
                        string("sample_type"),
                        integer("num_lines"),
                        string("mimetype")
                            .description("MIME type of the uploaded file."),
                        string("source"),
                        string("signature"))))
        .perform(MistralUploadFileAction::perform);

    private MistralUploadFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("https://api.mistral.ai/v1/files"))
            .body(
                Http.Body.of(
                    Map.of(
                        PURPOSE, inputParameters.getString(PURPOSE),
                        FILE, inputParameters.getRequiredFileEntry(FILE)),
                    Http.BodyContentType.FORM_DATA))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
