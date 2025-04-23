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

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.TYPE;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.URL;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class MistralOcrAction {

    public enum DocumentType {

        DOCUMENT_URL("document_url"),
        IMAGE_URL("image_url");

        private final String value;

        DocumentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("ocr")
        .title("Document OCR")
        .description("Extracts text and structured content from documents.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("Model to use.")
                .defaultValue("mistral-ocr-latest")
                .required(true),
            string(TYPE)
                .label("Type")
                .description("Type of the document to run OCR on.")
                .options(
                    option("Image", DocumentType.IMAGE_URL.getValue()),
                    option("PDF", DocumentType.DOCUMENT_URL.getValue()))
                .defaultValue(DocumentType.IMAGE_URL.getValue())
                .required(true),
            string(URL)
                .label("Image URL")
                .description("Url of the image to run OCR on.")
                .displayCondition("%s == '%s'".formatted(TYPE, DocumentType.IMAGE_URL.getValue()))
                .required(true),
            string(URL)
                .label("Document URL")
                .description("Url of the document to run OCR on.")
                .displayCondition("%s == '%s'".formatted(TYPE, DocumentType.DOCUMENT_URL.getValue()))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("pages")
                            .items(
                                object()
                                    .properties(
                                        integer("index"),
                                        string("markdown"),
                                        array("images")
                                            .items(
                                                object()
                                                    .properties(
                                                        string("id"),
                                                        integer("top_left_x"),
                                                        integer("top_left_y"),
                                                        integer("bottom_right_x"),
                                                        integer("bottom_right_y"))),
                                        object("dimensions")
                                            .properties(
                                                integer("dpi"),
                                                integer("height"),
                                                integer("width")))),
                        string("model"),
                        object("usage_info")
                            .properties(
                                integer("pages_processed"),
                                integer("doc_size_bytes")))))
        .perform(MistralOcrAction::perform);

    private MistralOcrAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String type = inputParameters.getRequiredString(TYPE);

        return context.http(http -> http.post("https://api.mistral.ai/v1/ocr"))
            .body(
                Http.Body.of(
                    MODEL, inputParameters.getRequiredString(MODEL),
                    "document", Map.of(TYPE, type, type, inputParameters.getRequiredString(URL))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
