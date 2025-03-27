/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.example.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.example.constant.CryptoHelperConstants.ALPHANUMERIC_CHARACTERS;
import static com.bytechef.component.example.constant.CryptoHelperConstants.CHARACTER_SET;
import static com.bytechef.component.example.constant.CryptoHelperConstants.LENGTH;
import static com.bytechef.component.example.constant.CryptoHelperConstants.SYMBOL_CHARACTERS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperGeneratePasswordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("generatePassword")
        .title("Generate Password")
        .description("Generate a random password of the specified length.")
        .properties(
            integer(LENGTH)
                .label("Length")
                .description("The length of the password.")
                .defaultValue(8)
                .required(true),
            string(CHARACTER_SET)
                .label("Character Set")
                .description("The character set to be used for generating the password.")
                .required(true)
                .options(
                    option("Alphanumeric", ALPHANUMERIC_CHARACTERS),
                    option("Alphanumeric + Symbols", ALPHANUMERIC_CHARACTERS + SYMBOL_CHARACTERS)))
        .output(
            outputSchema(
                string()
                    .description("Generated password")))
        .perform(CryptoHelperGeneratePasswordAction::perform);

    private CryptoHelperGeneratePasswordAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < inputParameters.getRequiredInteger(LENGTH); i++) {
            String characterSet = inputParameters.getRequiredString(CHARACTER_SET);

            int randomIndex = (int) Math.floor(Math.random() * characterSet.length());

            password.append(characterSet.charAt(randomIndex));
        }

        return password.toString();
    }
}
