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
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PASSPHRASE;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PRIVATE_KEY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Iterator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperPGPDecryptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("PGPdecrypt")
        .title("PGP Decrypt")
        .description("Decrypts PGP encrypted file using private key and passphrase.")
        .properties(
            string(PRIVATE_KEY)
                .label("Private PGP Key")
                .description(
                    "Private PGP key that will decrypt the file. Make sure there is a new line after the PGP header.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File")
                .description("File that will be decrypted.")
                .required(true),
            string(PASSPHRASE)
                .label("Passphrase")
                .description("Passphrase that was used for encryption.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("Decrypted file.")))
        .perform(CryptoHelperPGPDecryptAction::perform);

    private CryptoHelperPGPDecryptAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        FileEntry encryptedFileEntry = inputParameters.getRequiredFileEntry(FILE);
        byte[] encryptedBytes = context.file(file -> file.readAllBytes(encryptedFileEntry));

        ByteArrayInputStream encryptedInputStream = new ByteArrayInputStream(encryptedBytes);

        PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(
            PGPUtil.getDecoderStream(encryptedInputStream), new JcaKeyFingerprintCalculator());
        PGPEncryptedDataList pgpEncryptedDataList = (PGPEncryptedDataList) pgpObjectFactory.nextObject();

        PGPPublicKeyEncryptedData pgpPublicKeyEncryptedData = (PGPPublicKeyEncryptedData) pgpEncryptedDataList.get(0);

        PGPPrivateKey pgpPrivateKey = getPrivateKey(
            inputParameters.getRequiredString(PRIVATE_KEY), inputParameters.getRequiredString(PASSPHRASE),
            pgpEncryptedDataList);

        InputStream decryptedDataStream =
            pgpPublicKeyEncryptedData.getDataStream(
                new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(pgpPrivateKey));

        ByteArrayOutputStream decryptedOutputStream = getDecryptedDataFromStream(decryptedDataStream);

        return context.file(
            file -> file.storeContent("decrypted.", new ByteArrayInputStream(decryptedOutputStream.toByteArray())));
    }

    private static PGPPrivateKey getPrivateKey(
        String privateKeyString, String passphrase, PGPEncryptedDataList encDataList)
        throws IOException, PGPException {

        InputStream privateKeyInputStream = new ByteArrayInputStream(privateKeyString.getBytes(StandardCharsets.UTF_8));

        PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(privateKeyInputStream), new JcaKeyFingerprintCalculator());

        PGPPrivateKey privateKey = null;

        Iterator<PGPEncryptedData> iterator = encDataList.getEncryptedDataObjects();

        while (privateKey == null && iterator.hasNext()) {
            PGPPublicKeyEncryptedData pgpPublicKeyEncryptedData = (PGPPublicKeyEncryptedData) iterator.next();
            PGPSecretKey pgpSecKey = pgpSecretKeyRingCollection.getSecretKey(pgpPublicKeyEncryptedData.getKeyID());

            privateKey = pgpSecKey == null ? null : pgpSecKey.extractPrivateKey(
                new JcePBESecretKeyDecryptorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(passphrase.toCharArray()));
        }

        return privateKey;
    }

    private static ByteArrayOutputStream getDecryptedDataFromStream(InputStream decryptedDataStream)
        throws IOException {
        ByteArrayOutputStream decryptedData = new ByteArrayOutputStream();

        int ch;

        while ((ch = decryptedDataStream.read()) >= 0) {
            decryptedData.write(ch);
        }

        return decryptedData;
    }
}
