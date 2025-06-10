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
                            .description("List of OCR info for pages.")
                            .items(
                                object()
                                    .properties(
                                        integer("index")
                                            .description("The page index in a pdf document starting from 0."),
                                        string("markdown")
                                            .description("The markdown string response of the page."),
                                        array("images")
                                            .description("List of all extracted images in the page.")
                                            .items(
                                                object()
                                                    .properties(
                                                        string("id")
                                                            .description("Image ID for extracted image in a page."),
                                                        integer("top_left_x")
                                                            .description(
                                                                "X coordinate of top-left corner of the extracted " +
                                                                    "image."),
                                                        integer("top_left_y")
                                                            .description(
                                                                "Y coordinate of top-left corner of the extracted " +
                                                                    "image."),
                                                        integer("bottom_right_x")
                                                            .description(
                                                                "X coordinate of bottom-right corner of the " +
                                                                    "extracted image."),
                                                        integer("bottom_right_y")
                                                            .description(
                                                                "Y coordinate of bottom-right corner of the " +
                                                                    "extracted image."))),
                                        object("dimensions")
                                            .description("The dimensions of the PDF Page's screenshot image.")
                                            .properties(
                                                integer("dpi")
                                                    .description("Dots per inch of the page-image."),
                                                integer("height")
                                                    .description("Height of the image in pixels."),
                                                integer("width")
                                                    .description("Width of the image in pixels.")))),
                        string("model")
                            .description("The model used to generate the OCR."),
                        object("usage_info")
                            .description("Usage info for the OCR request.")
                            .properties(
                                integer("pages_processed")
                                    .description("Number of pages processed."),
                                integer("doc_size_bytes")
                                    .description("Document size in bytes.")))))
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
