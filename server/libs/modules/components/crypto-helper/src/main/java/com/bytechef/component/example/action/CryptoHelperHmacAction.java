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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.example.constant.CryptoHelperConstants.ALGORITHM;
import static com.bytechef.component.example.constant.CryptoHelperConstants.INPUT;
import static com.bytechef.component.example.constant.CryptoHelperConstants.KEY;
import static com.bytechef.component.example.util.CryptoHelperUtil.bytesToHex;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.example.util.CryptoHelperUtil;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperHmacAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("hmacAction")
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
        .output(
            outputSchema(
                string()
                    .description("Hmac value of the input")))
        .perform(CryptoHelperHmacAction::perform);

    private CryptoHelperHmacAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        String algorithm = inputParameters.getRequiredString(ALGORITHM);

        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(inputParameters.getRequiredString(KEY)
                .getBytes(StandardCharsets.UTF_8), algorithm);

            mac.init(secretKey);

            byte[] hmac = mac.doFinal(inputParameters.getRequiredString(INPUT)
                .getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hmac);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
