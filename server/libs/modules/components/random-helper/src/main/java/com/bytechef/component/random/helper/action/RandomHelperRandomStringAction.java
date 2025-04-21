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

package com.bytechef.component.random.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.ALPHANUMERIC_CHARACTERS;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.CHARACTER_SET;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.LENGTH;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.SYMBOL_CHARACTERS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class RandomHelperRandomStringAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("randomString")
        .title("Random String")
        .description("Generates a random string value.")
        .properties(
            integer(LENGTH)
                .label("Length")
                .description("The length of the generated string.")
                .defaultValue(8)
                .required(true),
            string(CHARACTER_SET)
                .label("Character Set")
                .description("The character set to be used for generating string.")
                .required(true)
                .options(
                    option("Alphanumeric", ALPHANUMERIC_CHARACTERS),
                    option("Alphanumeric + Symbols", ALPHANUMERIC_CHARACTERS + SYMBOL_CHARACTERS)))
        .output(
            outputSchema(
                string()
                    .description("Generated string.")))
        .perform(RandomHelperRandomStringAction::perform);

    private RandomHelperRandomStringAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        StringBuilder randomString = new StringBuilder();

        for (int i = 0; i < inputParameters.getRequiredInteger(LENGTH); i++) {
            String characterSet = inputParameters.getRequiredString(CHARACTER_SET);

            int randomIndex = (int) Math.floor(Math.random() * characterSet.length());

            randomString.append(characterSet.charAt(randomIndex));
        }

        return randomString.toString();
    }
}
