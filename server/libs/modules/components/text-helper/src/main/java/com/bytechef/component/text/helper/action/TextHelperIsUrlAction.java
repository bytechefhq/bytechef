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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_2_SLASHES;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_ALL_SCHEMES;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ALLOW_LOCAL_URLS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.NO_FRAGMENT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import org.apache.commons.validator.routines.UrlValidator;

/**
 * @author Nikolina Špehar
 */
public class TextHelperIsUrlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("isUrl")
        .title("Is URL?")
        .description("Check if a string is a valid URL.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The input text that will be checked.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            bool(ALLOW_2_SLASHES)
                .label("Allow 2 Slashes")
                .description("Allows double '/' characters in the path component.")
                .required(false)
                .defaultValue(false),
            bool(NO_FRAGMENT)
                .label("No Fragment")
                .description("Enabling this options disallows any URL fragments.")
                .required(false)
                .defaultValue(false),
            bool(ALLOW_ALL_SCHEMES)
                .label("Allow All Schemes")
                .description(
                    "Allows all validly formatted schemes to pass validation instead of supplying a set of valid " +
                        "schemes.")
                .required(false)
                .defaultValue(false),
            bool(ALLOW_LOCAL_URLS)
                .label("Allow Local URLs")
                .description("Allow local URLs, such as https://localhost/ or https://machine/ .")
                .required(false)
                .defaultValue(false))
        .output(outputSchema(bool().description("Whether the text is valid URL.")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#is-url")
        .perform(TextHelperIsUrlAction::perform);

    private TextHelperIsUrlAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String url = inputParameters.getRequiredString(TEXT);

        long allow2slashes = inputParameters.getBoolean(ALLOW_2_SLASHES) ? UrlValidator.ALLOW_2_SLASHES : 0L;
        long noFragment = inputParameters.getBoolean(NO_FRAGMENT) ? UrlValidator.NO_FRAGMENTS : 0L;
        long allowAllSchemes = inputParameters.getBoolean(ALLOW_ALL_SCHEMES) ? UrlValidator.ALLOW_ALL_SCHEMES : 0L;
        long allowLocalUrls = inputParameters.getBoolean(ALLOW_LOCAL_URLS) ? UrlValidator.ALLOW_LOCAL_URLS : 0L;

        UrlValidator urlValidator = new UrlValidator(allow2slashes + noFragment + allowAllSchemes + allowLocalUrls);

        return urlValidator.isValid(url);
    }
}
