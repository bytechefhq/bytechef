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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperPGPEncryptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("PGPencrypt")
        .title("PGP Encrypt")
        .description("Encrypts the file using PGP public key.")
        .properties(
            string(PUBLIC_KEY)
                .label("Public PGP Key")
                .description(
                    "Public PGP key of the recipient of the encrypted file. Make sure there is a new line after the PGP header.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File")
                .description("File that will be encrypted.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("PGP encrypted file.")))
        .perform(CryptoHelperPGPEncryptAction::perform);

    private CryptoHelperPGPEncryptAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException, PGPException {

        Security.addProvider(new BouncyCastleProvider());

        String publicKeyString = inputParameters.getRequiredString(PUBLIC_KEY);

        InputStream publicKeyInputStream = PGPUtil.getDecoderStream(
            new ByteArrayInputStream(publicKeyString.getBytes(StandardCharsets.UTF_8)));

        JcaPGPPublicKeyRingCollection pgpPublicKeyRingCollection = new JcaPGPPublicKeyRingCollection(
            publicKeyInputStream);

        publicKeyInputStream.close();

        PGPPublicKey pgpPublicKey = getPublicKey(pgpPublicKeyRingCollection);

        byte[] inputFileBytes = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        PGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .setSecureRandom(new SecureRandom())
            .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator pgpEncryptedDataGenerator = new PGPEncryptedDataGenerator(encryptorBuilder);

        pgpEncryptedDataGenerator.addMethod(
            new JcePublicKeyKeyEncryptionMethodGenerator(pgpPublicKey)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME));

        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();

        try (OutputStream cOut = pgpEncryptedDataGenerator.open(encryptedData, inputFileBytes.length)) {
            cOut.write(inputFileBytes);
        }

        String armoredMessage = armorMessage(encryptedData.toByteArray());

        return context.file(
            file -> file.storeContent("encrypted.",
                new ByteArrayInputStream(armoredMessage.getBytes(StandardCharsets.UTF_8))));
    }

    private static PGPPublicKey getPublicKey(JcaPGPPublicKeyRingCollection pgpPublicKeyRingCollection) {
        PGPPublicKey publicKey = null;
        Iterator<PGPPublicKeyRing> keyRingIterator = pgpPublicKeyRingCollection.getKeyRings();

        while (keyRingIterator.hasNext()) {
            PGPPublicKeyRing keyRing = keyRingIterator.next();
            Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();

            while (keyIter.hasNext()) {
                PGPPublicKey key = keyIter.next();

                if (key.isEncryptionKey()) {
                    publicKey = key;

                    break;
                }
            }
        }

        return publicKey;
    }

    private static String armorMessage(byte[] encryptedData) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(byteArrayOutputStream);

        armoredOutputStream.write(encryptedData);
        armoredOutputStream.close();

        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }
}
