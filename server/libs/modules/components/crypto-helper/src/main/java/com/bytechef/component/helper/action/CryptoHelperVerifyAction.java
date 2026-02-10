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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.FILE;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PUBLIC_KEY;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.SIGNATURE;
import static com.bytechef.component.helper.util.CryptoHelperUtil.getPublicRSAKeyFromString;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.security.Signature;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperVerifyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("verify")
        .title("Verify")
        .description("Verify the signature using public RSA key.")
        .properties(
            string(PUBLIC_KEY)
                .label("Public RSA Key")
                .description("Public RSA key that will verify the signature.")
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File object whose signature will be verified.")
                .required(true),
            fileEntry(SIGNATURE)
                .label("Signature")
                .description("Signature that will be verified.")
                .required(true))
        .output(outputSchema(bool().description("Outcome of the verification of the signature.")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#verify")
        .perform(CryptoHelperVerifyAction::perform);

    private CryptoHelperVerifyAction() {
    }

    protected static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        byte[] signatureBytes = context.file(
            file -> file.readAllBytes(inputParameters.getRequiredFileEntry(SIGNATURE)));

        Signature signature = Signature.getInstance("SHA256withRSA");

        signature.initVerify(getPublicRSAKeyFromString(inputParameters.getRequiredString(PUBLIC_KEY)));

        byte[] fileBytes = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        signature.update(fileBytes);

        return signature.verify(signatureBytes);
    }
}
