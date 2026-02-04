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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.regex.Pattern;

/**
 * @author Nikolina Špehar
 */
public class TextHelperIsDomainAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("isDomain")
        .title("Is Domain?")
        .description("Check if a string is a valid domain.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text to be checked as a valid domain name.")
                .required(true))
        .output(outputSchema(bool().description("Whether the text is a valid domain.")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#is-domain")
        .perform(TextHelperIsDomainAction::perform);

    private TextHelperIsDomainAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String domainRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)"
            + "(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$";

        return Pattern.compile(domainRegex)
            .matcher(inputParameters.getRequiredString(TEXT))
            .matches();
    }
}
