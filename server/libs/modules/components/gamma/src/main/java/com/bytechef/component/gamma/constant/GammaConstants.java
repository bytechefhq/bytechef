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

package com.bytechef.component.gamma.constant;

import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Ivica Cardic
 */
public class GammaConstants {

    public static final String ADDITIONAL_INSTRUCTIONS = "additionalInstructions";
    public static final String EXPORT_AS = "exportAs";
    public static final String FORMAT = "format";
    public static final String GENERATION_ID = "generationId";
    public static final String ID = "id";
    public static final String INPUT_TEXT = "inputText";
    public static final String NAME = "name";
    public static final String NUM_CARDS = "numCards";
    public static final String TEXT_MODE = "textMode";
    public static final String THEME_ID = "themeId";

    public static final ModifiableObjectProperty GENERATION_OUTPUT_PROPERTY = object()
        .properties(
            string(GENERATION_ID)
                .description("The ID of the generation."),
            string("status")
                .description("The status of the generation: pending, completed or failed."),
            string("gammaId")
                .description("The ID of the generated gamma, present when the generation is completed."),
            string("gammaUrl")
                .description("The URL of the generated gamma, present when the generation is completed."),
            string("exportUrl")
                .description(
                    "The download URL of the exported file, present when Export As was set and the generation is " +
                        "completed. Anyone with this URL can download the file until it expires."),
            object("error")
                .description("The error details, present when the generation failed.")
                .properties(
                    string("message")
                        .description("The error message."),
                    number("statusCode")
                        .description("The HTTP status code of the error.")),
            object("credits")
                .description("The credit usage of the generation.")
                .properties(
                    number("deducted")
                        .description("The number of credits deducted for this generation."),
                    number("remaining")
                        .description("The number of credits remaining in the workspace.")));

    private GammaConstants() {
    }
}
