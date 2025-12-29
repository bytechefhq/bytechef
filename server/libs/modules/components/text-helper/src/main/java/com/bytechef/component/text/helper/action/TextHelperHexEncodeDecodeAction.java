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
        .perform(TextHelperHexEncodeDecodeAction::perform);

    private TextHelperHexEncodeDecodeAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        if (text.isEmpty()) {
            return text;
        }

        String operation = inputParameters.getRequiredString(OPERATION);

        if (operation.equals(OperationType.ENCODE.name())) {
            return hexEncodeString(text);
        } else {
            return hexDecodeString(text);
        }
    }

    private static String hexDecodeString(String encoded) {
        if (encoded.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }

        byte[] decodedBytes = new byte[encoded.length() / 2];
        for (int i = 0; i < encoded.length(); i += 2) {
            decodedBytes[i / 2] = (byte) Integer.parseInt(encoded.substring(i, i + 2), 16);
        }

        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private static String hexEncodeString(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexEncodedStringBuilder = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            hexEncodedStringBuilder.append(String.format("%02x", b));
        }

        return hexEncodedStringBuilder.toString();
    }

}
