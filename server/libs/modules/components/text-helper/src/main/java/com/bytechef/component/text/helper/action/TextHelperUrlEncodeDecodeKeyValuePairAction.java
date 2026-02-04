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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.KEY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.OPERATION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.OPERATION_PROPERTY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PAIRS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.VALUE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.text.helper.constant.OperationType;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nikolina Špehar
 */
public class TextHelperUrlEncodeDecodeKeyValuePairAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("urlEncodeDecodeKeyValuePair")
        .title("URL Encode/Decode Key/Value Pair")
        .description("URL encode/decode a specified set of key/value pairs.")
        .properties(
            array(PAIRS)
                .label("Pairs")
                .description("Key/Value pairs that will be encoded.")
                .required(true)
                .items(
                    object()
                        .properties(
                            string(KEY)
                                .label("Key")
                                .description("Key in the key/value pair.")
                                .required(true),
                            string(VALUE)
                                .label("Value")
                                .description("Value in the key/value pair.")
                                .required(true))),
            OPERATION_PROPERTY)
        .output(outputSchema(string().description("URL encoded/decoded content.")))
        .help(
            "",
            "https://docs.bytechef.io/reference/components/text-helper_v1#url-encodedecode-keyvalue-pair")
        .perform(TextHelperUrlEncodeDecodeKeyValuePairAction::perform);

    private TextHelperUrlEncodeDecodeKeyValuePairAction() {
    }

    public static Map<String, String> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, String> pairs = inputParameters.getRequiredMap(PAIRS, String.class);

        String operation = inputParameters.getRequiredString(OPERATION);

        if (operation.equals(OperationType.ENCODE.name())) {
            return encodeKeyValuePairs(pairs);

        } else {
            return decodeKeyValuePairs(pairs);
        }
    }

    private static Map<String, String> encodeKeyValuePairs(Map<String, String> pairs) {
        return pairs.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                entry -> URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)));
    }

    private static Map<String, String> decodeKeyValuePairs(Map<String, String> pairs) {
        return pairs.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> URLDecoder.decode(entry.getKey(), StandardCharsets.UTF_8),
                entry -> URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8)));
    }
}
