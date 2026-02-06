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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.LENGTH;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.RANDOM;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class RandomHelperRandomHexAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("randomHex")
        .title("Random Hex")
        .description("Generates a random Hex.")
        .help("", "https://docs.bytechef.io/reference/components/random-helper_v1#random-hex")
        .properties(
            integer(LENGTH)
                .label("Hex Byte Length")
                .description("Hex byte length must be a positive integer smaller than or equal to 32.")
                .maxValue(32)
                .required(true))
        .output(
            outputSchema(
                string()
                    .description("Generated hex.")))
        .perform(RandomHelperRandomHexAction::perform);

    private RandomHelperRandomHexAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        int length = inputParameters.getRequiredInteger(LENGTH);
        StringBuilder hexString = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            hexString.append(Integer.toHexString(RANDOM.nextInt(16)));
        }

        return hexString.toString();
    }
}
