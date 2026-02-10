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

package com.bytechef.component.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.ALGORITHM;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.INPUT;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.KEY;
import static com.bytechef.component.helper.util.CryptoHelperUtil.convertBytesToHexString;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.helper.util.CryptoHelperUtil;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperHmacAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("hmac")
        .title("Hmac")
        .description("Computes and returns the HMAC of the input.")
        .properties(
            string(ALGORITHM)
                .label("Cryptographic Algorithm")
                .description("The cryptographic algorithm that will be used to hash the input.")
                .options(CryptoHelperUtil.getHmacAlgorithmOptions())
                .required(true),
            string(INPUT)
                .label("Input")
                .description("Generates a cryptographic HMAC for the provided input.")
                .required(true),
            string(KEY)
                .label("Key")
                .description("Key that will be used for the encryption.")
                .required(true))
        .output(outputSchema(string().description("Hmac value of the input")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#hmac")
        .perform(CryptoHelperHmacAction::perform);

    private CryptoHelperHmacAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        try {
            String key = inputParameters.getRequiredString(KEY);
            String algorithm = inputParameters.getRequiredString(ALGORITHM);

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);

            Mac mac = Mac.getInstance(algorithm);

            mac.init(secretKey);

            String input = inputParameters.getRequiredString(INPUT);

            return convertBytesToHexString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
