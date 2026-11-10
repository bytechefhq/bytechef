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

package com.bytechef.component.gamma.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.gamma.constant.GammaConstants.ADDITIONAL_INSTRUCTIONS;
import static com.bytechef.component.gamma.constant.GammaConstants.EXPORT_AS;
import static com.bytechef.component.gamma.constant.GammaConstants.FORMAT;
import static com.bytechef.component.gamma.constant.GammaConstants.GENERATION_ID;
import static com.bytechef.component.gamma.constant.GammaConstants.INPUT_TEXT;
import static com.bytechef.component.gamma.constant.GammaConstants.NUM_CARDS;
import static com.bytechef.component.gamma.constant.GammaConstants.TEXT_MODE;
import static com.bytechef.component.gamma.constant.GammaConstants.THEME_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.gamma.util.GammaUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GammaGeneratePresentationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("generatePresentation")
        .title("Generate Presentation")
        .description(
            "Start generating a presentation, document, or social post from text. Generation runs asynchronously; " +
                "pass the returned generation ID to Get Generation to check its status and get the gamma URL and " +
                "export URL.")
        .properties(
            string(INPUT_TEXT)
                .label("Input Text")
                .description("The text to generate from: a topic, an outline, or full content.")
                .controlType(ControlType.TEXT_AREA)
                .minLength(1)
                .maxLength(400000)
                .required(true),
            string(TEXT_MODE)
                .label("Text Mode")
                .description(
                    "How to interpret the input text: generate expands it into new content, condense summarizes " +
                        "it, and preserve keeps it as written.")
                .options(
                    option("Generate", "generate"),
                    option("Condense", "condense"),
                    option("Preserve", "preserve"))
                .defaultValue("generate")
                .required(true),
            string(FORMAT)
                .label("Format")
                .description("The output format of the generated gamma.")
                .options(
                    option("Presentation", "presentation"),
                    option("Document", "document"),
                    option("Social", "social"))
                .defaultValue("presentation")
                .required(false),
            string(THEME_ID)
                .label("Theme ID")
                .description("The ID of the theme to apply. Uses the workspace default theme when empty.")
                .options((OptionsFunction<String>) GammaUtils::getThemeOptions)
                .required(false),
            integer(NUM_CARDS)
                .label("Number of Cards")
                .description("The target number of cards or slides to generate.")
                .minValue(1)
                .required(false),
            string(ADDITIONAL_INSTRUCTIONS)
                .label("Additional Instructions")
                .description("Additional instructions for the AI, for example about tone, layout or content focus.")
                .controlType(ControlType.TEXT_AREA)
                .maxLength(5000)
                .required(false),
            string(EXPORT_AS)
                .label("Export As")
                .description(
                    "The file format to export the gamma to after generation. Get Generation returns the download " +
                        "URL as the export URL.")
                .options(
                    option("PDF", "pdf"),
                    option("PPTX", "pptx"))
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(GENERATION_ID)
                            .description("The ID of the generation. Pass it to Get Generation."),
                        string("warnings")
                            .description("Warnings about the request, for example ignored options."))))
        .help("", "https://docs.bytechef.io/reference/components/gamma_v1#generate-presentation")
        .perform(GammaGeneratePresentationAction::perform);

    private GammaGeneratePresentationAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.http(http -> http.post("/generations"))
            .body(
                Body.of(
                    INPUT_TEXT, inputParameters.getRequiredString(INPUT_TEXT),
                    TEXT_MODE, inputParameters.getRequiredString(TEXT_MODE),
                    FORMAT, inputParameters.getString(FORMAT),
                    THEME_ID, inputParameters.getString(THEME_ID),
                    NUM_CARDS, inputParameters.getInteger(NUM_CARDS),
                    ADDITIONAL_INSTRUCTIONS, inputParameters.getString(ADDITIONAL_INSTRUCTIONS),
                    EXPORT_AS, inputParameters.getString(EXPORT_AS)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
