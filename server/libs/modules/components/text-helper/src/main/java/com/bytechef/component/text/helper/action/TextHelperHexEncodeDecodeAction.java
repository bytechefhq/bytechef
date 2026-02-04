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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.OPERATION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.OPERATION_PROPERTY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.text.helper.constant.OperationType;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * @author Nikolina Špehar
 */
public class TextHelperHexEncodeDecodeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("hexEncodeDecode")
        .title("Hex Encode/Decode")
        .description("Hex encode/decode a specified string.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be encoded or decoded.")
                .required(true),
            OPERATION_PROPERTY)
        .output(outputSchema(string().description("Hex encoded/decoded content.")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#hex-encodedecode")
        .perform(TextHelperHexEncodeDecodeAction::perform);

    private TextHelperHexEncodeDecodeAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        if (text.isEmpty()) {
            return text;
        }

        String operation = inputParameters.getRequiredString(OPERATION);
        HexFormat hexFormat = HexFormat.of();

        if (Objects.equals(operation, OperationType.ENCODE.name())) {
            return hexFormat.formatHex(text.getBytes(StandardCharsets.UTF_8));
        } else {
            return new String(hexFormat.parseHex(text), StandardCharsets.UTF_8);
        }
    }
}
