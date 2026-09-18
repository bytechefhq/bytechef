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

package com.bytechef.component.neon.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.neon.constant.NeonConstants.KEY_ID;
import static com.bytechef.component.neon.constant.NeonConstants.PRIVATE_KEY;
import static com.bytechef.component.neon.constant.NeonConstants.SUBJECT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NeonGenerateServiceAccountAction {

    private static final String JWKS = "jwks";
    private static final String PUBLIC_KEY = "publicKey";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("generateServiceAccount")
        .title("Generate Service Account")
        .description(
            "Generates an RSA key pair, a JWKS document for the public key, and a random subject — everything " +
                "needed to configure the connection's Self-Signed JWT authorization method, without running " +
                "external tools.")
        .properties(
            string(KEY_ID)
                .label("Key Id")
                .description(
                    "Used as the JWT header's \"kid\" and the JWKS entry's \"kid\". Randomly generated if left " +
                        "empty.")
                .required(false),
            string(SUBJECT)
                .label("Subject")
                .description("Used as the JWT's \"sub\" claim. Randomly generated if left empty.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(PRIVATE_KEY)
                            .description(
                                "PKCS#8 PEM-encoded RSA private key. Paste this into the connection's Private " +
                                    "Key field."),
                        string(PUBLIC_KEY)
                            .description("PEM-encoded RSA public key, for reference."),
                        string(KEY_ID)
                            .description("The key id used in the JWT header and the JWKS document."),
                        string(SUBJECT)
                            .description("The subject to use in the connection's Subject field."),
                        object(JWKS)
                            .description(
                                "JWKS document to publish and register with Neon as a custom auth provider.")
                            .properties(
                                array("keys")
                                    .items(
                                        object()
                                            .properties(
                                                string("kty"), string("use"), string("alg"), string("kid"),
                                                string("n"), string("e")))))))
        .perform(NeonGenerateServiceAccountAction::perform);

    private NeonGenerateServiceAccountAction() {
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String keyId = inputParameters.getString(KEY_ID);

        if (keyId == null || keyId.isBlank()) {
            keyId = UUID.randomUUID()
                .toString();
        }

        String subject = inputParameters.getString(SUBJECT);

        if (subject == null || subject.isBlank()) {
            subject = UUID.randomUUID()
                .toString();
        }

        return Map.of(
            PRIVATE_KEY, toPem("PRIVATE KEY", keyPair.getPrivate()
                .getEncoded()),
            PUBLIC_KEY, toPem("PUBLIC KEY", keyPair.getPublic()
                .getEncoded()),
            KEY_ID, keyId,
            SUBJECT, subject,
            JWKS, toJwks((RSAPublicKey) keyPair.getPublic(), keyId));
    }

    private static String toPem(String label, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
            .encodeToString(encoded);

        return "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
    }

    private static Map<String, ?> toJwks(RSAPublicKey publicKey, String keyId) {
        Map<String, Object> jwk = new LinkedHashMap<>();

        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);
        jwk.put("n", toBase64Url(publicKey.getModulus()));
        jwk.put("e", toBase64Url(publicKey.getPublicExponent()));

        return Map.of("keys", List.of(jwk));
    }

    private static String toBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }
}
