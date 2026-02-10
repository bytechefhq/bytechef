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
import java.io.ByteArrayOutputStream;
import javax.crypto.Cipher;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperRsaDecryptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rsaDecrypt")
        .title("RSA Decrypt")
        .description("Decrypts RSA encrypted file using RSA private key.")
        .properties(
            string(PRIVATE_KEY)
                .label("Private RSA Key")
                .description("Private RSA key that will decrypt the file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File Entry")
                .description("File object with content that will be decrypted.")
                .required(true))
        .output(outputSchema(fileEntry().description("File object with decrypted content.")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#rsa-decrypt")
        .perform(CryptoHelperRsaDecryptAction::perform);

    private CryptoHelperRsaDecryptAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.DECRYPT_MODE, getPrivateRSAKeyFromString(inputParameters.getRequiredString(PRIVATE_KEY)));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int maxBlockSize = 256;
        int offset = 0;

        byte[] encryptedData = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        while (offset < encryptedData.length) {
            int chunkSize = Math.min(maxBlockSize, encryptedData.length - offset);
            byte[] chunk = cipher.doFinal(encryptedData, offset, chunkSize);
            byteArrayOutputStream.write(chunk);
            offset += chunkSize;
        }

        return context.file(
            file -> file.storeContent("decrypted.", new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
    }
}
