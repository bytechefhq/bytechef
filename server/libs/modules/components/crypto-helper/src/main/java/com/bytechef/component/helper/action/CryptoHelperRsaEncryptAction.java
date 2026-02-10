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
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PUBLIC_KEY;
import static com.bytechef.component.helper.util.CryptoHelperUtil.getPublicRSAKeyFromString;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.io.ByteArrayInputStream;
import javax.crypto.Cipher;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperRsaEncryptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rsaEncrypt")
        .title("RSA Encrypt")
        .description("Encrypts the file using the RSA public key.")
        .properties(
            string(PUBLIC_KEY)
                .label("Public RSA Key")
                .description("Public RSA key of the recipient of the encrypted file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File object with content that will be encrypted.")
                .required(true))
        .output(outputSchema(fileEntry().description("RSA encrypted file.")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#rsa-encrypt")
        .perform(CryptoHelperRsaEncryptAction::perform);

    private CryptoHelperRsaEncryptAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, getPublicRSAKeyFromString(inputParameters.getRequiredString(PUBLIC_KEY)));

        byte[] encryptedBytes = cipher.doFinal(
            context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE))));

        return context.file(
            file -> file.storeContent("encrypted.", new ByteArrayInputStream(encryptedBytes)));
    }
}
