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
import static com.bytechef.component.example.util.CryptoHelperUtil.bytesToHex;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.example.util.CryptoHelperUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperHashAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("hashAction")
        .title("Hash")
        .description("Computes and returns the hash of the input.")
        .properties(
            string(ALGORITHM)
                .label("Cryptographic Algorithm")
                .description("The cryptographic algorithm that will be used to hash the input.")
                .options(CryptoHelperUtil.getHashAlgorithmOptions())
                .required(true),
            string(INPUT)
                .label("Input")
                .description("Calculates the hash of the provided input.")
                .required(true))
        .output(
            outputSchema(
                string()
                    .description("Hashed value of the input")))
        .perform(CryptoHelperHashAction::perform);

    private CryptoHelperHashAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        try {
            MessageDigest digest = MessageDigest.getInstance(inputParameters.getRequiredString(ALGORITHM));
            byte[] hashBytes = digest.digest(inputParameters.getRequiredString(INPUT)
                .getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid Hash Algorithm", e);
        }
    }
}
