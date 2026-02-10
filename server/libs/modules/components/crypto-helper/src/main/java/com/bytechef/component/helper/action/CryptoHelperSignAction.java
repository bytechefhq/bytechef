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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.FILE;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PRIVATE_KEY;
import static com.bytechef.component.helper.util.CryptoHelperUtil.getPrivateRSAKeyFromString;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.io.ByteArrayInputStream;
import java.security.Signature;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperSignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sign")
        .title("Sign")
        .description("Cryptographically signs a file.")
        .properties(
            string(PRIVATE_KEY)
                .label("Private RSA Key")
                .description("Private RSA key that will be used to sign the file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File object with content that will be signed")
                .required(true))
        .output(outputSchema(fileEntry().description("Signature of the file.")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#sign")
        .perform(CryptoHelperSignAction::perform);

    private CryptoHelperSignAction() {
    }

    protected static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Signature signature = Signature.getInstance("SHA256withRSA");

        signature.initSign(getPrivateRSAKeyFromString(inputParameters.getRequiredString(PRIVATE_KEY)));

        byte[] fileBytes = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        signature.update(fileBytes);

        return context.file(file -> file.storeContent("signature.", new ByteArrayInputStream(signature.sign())));
    }
}
