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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nikolina Špehar
 */
public class TextHelperGetDomainFromUrlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDomainFromUrl")
        .title("Get Domain From URL")
        .description("Extracts domain from the given URL.")
        .properties(
            string(TEXT)
                .description("The URL you want to extract domain from.")
                .label("URL")
                .required(true))
        .output(outputSchema(string().description("Extracted domain")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#get-domain-from-url")
        .perform(TextHelperGetDomainFromUrlAction::perform);

    private TextHelperGetDomainFromUrlAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws MalformedURLException {

        Pattern pattern = Pattern.compile("^(?:https?://)?(?:www\\.)?([^:/\\n?]+)");
        Matcher matcher = pattern.matcher(inputParameters.getRequiredString(TEXT));

        String domain = "";

        if (matcher.find()) {
            domain = matcher.group(1);
        }

        return domain;
    }
}
