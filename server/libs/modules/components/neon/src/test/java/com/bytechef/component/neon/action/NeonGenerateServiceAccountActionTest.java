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

import static com.bytechef.component.neon.constant.NeonConstants.KEY_ID;
import static com.bytechef.component.neon.constant.NeonConstants.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NeonGenerateServiceAccountActionTest {

    @Test
    @SuppressWarnings("unchecked")
    void testPerformWithGivenKeyIdAndSubject() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(KEY_ID, "key-1", SUBJECT, "service-account"));

        Map<String, ?> result = NeonGenerateServiceAccountAction.perform(inputParameters, null, null);

        assertEquals("key-1", result.get(KEY_ID));
        assertEquals("service-account", result.get(SUBJECT));

        String privateKey = (String) result.get("privateKey");
        String publicKey = (String) result.get("publicKey");

        assertTrue(privateKey.startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(publicKey.startsWith("-----BEGIN PUBLIC KEY-----"));

        String publicKeyBase64 = publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

        PublicKey decodedPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(Base64.getDecoder()
                .decode(publicKeyBase64)));

        assertNotNull(decodedPublicKey);

        Map<String, ?> jwks = (Map<String, ?>) result.get("jwks");
        List<?> keys = (List<?>) jwks.get("keys");

        assertEquals(1, keys.size());

        Map<?, ?> jwk = (Map<?, ?>) keys.get(0);

        assertEquals("RSA", jwk.get("kty"));
        assertEquals("sig", jwk.get("use"));
        assertEquals("RS256", jwk.get("alg"));
        assertEquals("key-1", jwk.get("kid"));
        assertNotNull(jwk.get("n"));
        assertNotNull(jwk.get("e"));
    }

    @Test
    void testPerformGeneratesRandomKeyIdAndSubjectWhenMissing() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, ?> result = NeonGenerateServiceAccountAction.perform(inputParameters, null, null);

        assertNotNull(result.get(KEY_ID));
        assertNotNull(result.get(SUBJECT));
        assertTrue(((String) result.get(KEY_ID)).length() > 0);
        assertTrue(((String) result.get(SUBJECT)).length() > 0);
    }
}
