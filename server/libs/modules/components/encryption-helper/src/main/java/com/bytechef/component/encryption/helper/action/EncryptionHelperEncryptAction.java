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

package com.bytechef.component.encryption.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.encryption.helper.constant.EncryptionHelperConstants.FILE;
import static com.bytechef.component.encryption.helper.constant.EncryptionHelperConstants.PUBLIC_KEY;

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
public class EncryptionHelperEncryptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("encrypt")
        .title("Encrypt")
        .description("PGP encrypts the file using public key.")
        .properties(
            string(PUBLIC_KEY)
                .label("Public PGP Key")
                .description(
                    "Public PGP key of the recipient of the encrypted file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            fileEntry(FILE)
                .label("File")
                .description("File that will be encrypted.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("PGP encryption of the file.")))
        .perform(EncryptionHelperEncryptAction::perform);

    private EncryptionHelperEncryptAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException, PGPException {

        Security.addProvider(new BouncyCastleProvider());

        String publicKeyString = inputParameters.getRequiredString(PUBLIC_KEY);

        InputStream publicKeyInputStream = PGPUtil.getDecoderStream(
            new ByteArrayInputStream(publicKeyString.getBytes(StandardCharsets.UTF_8)));

        JcaPGPPublicKeyRingCollection pgpPublicKeyRingCollection =
            new JcaPGPPublicKeyRingCollection(publicKeyInputStream);
        publicKeyInputStream.close();

        PGPPublicKey publicKey = getPublicKey(pgpPublicKeyRingCollection);

        byte[] inputFileBytes = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(FILE)));

        PGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .setSecureRandom(new SecureRandom())
            .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encryptorBuilder);
        encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey)
            .setProvider(BouncyCastleProvider.PROVIDER_NAME));

        ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();

        try (OutputStream cOut = encGen.open(encryptedData, inputFileBytes.length)) {
            cOut.write(inputFileBytes);
        }

        String armoredMessage = armorMessage(encryptedData.toByteArray());

        return context.file(file -> file.storeContent("encrypted.",
            new ByteArrayInputStream(armoredMessage.getBytes(StandardCharsets.UTF_8))));
    }

    private static PGPPublicKey getPublicKey(JcaPGPPublicKeyRingCollection pgpPublicKeyRingCollection)
        throws IOException, PGPException {

        PGPPublicKey publicKey = null;

        Iterator<PGPPublicKeyRing> keyRingIter = pgpPublicKeyRingCollection.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = keyRingIter.next();
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
        ByteArrayOutputStream armoredStream = new ByteArrayOutputStream();
        ArmoredOutputStream armoredOut = new ArmoredOutputStream(armoredStream);

        armoredOut.write(encryptedData);
        armoredOut.close();

        return armoredStream.toString(StandardCharsets.UTF_8);
    }
}
