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

package com.bytechef.ee.embedded.security.service;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class SigningKeyServiceImpl implements SigningKeyService {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder();
    private static final KeyPairGenerator keyPairGenerator;

    static {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final SigningKeyRepository signingKeyRepository;

    public SigningKeyServiceImpl(SigningKeyRepository signingKeyRepository) {
        this.signingKeyRepository = signingKeyRepository;
    }

    @Override
    public String create(SigningKey signingKey) {
        Assert.notNull(signingKey, "'signingKey' must not be null");
        Assert.isTrue(signingKey.getId() == null, "'id' must be null");
        Assert.notNull(signingKey.getName(), "'name' must not be null");
        Assert.notNull(signingKey.getType(), "'type' must not be null");

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        signingKey.setKeyId(String.valueOf(TenantKey.of()));

        PublicKey publicKey = keyPair.getPublic();

        signingKey.setPublicKey(keyToPublicString(publicKey));

        signingKeyRepository.save(signingKey);

        PrivateKey privateKey = keyPair.getPrivate();

        return keyToPrivateString(privateKey);
    }

    @Override
    public void delete(long id) {
        signingKeyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKey getPublicKey(String keyId, long environmentId) {
        SigningKey signingKey = signingKeyRepository.findByKeyIdAndEnvironment(keyId, (int) environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Signing key not found for keyId: " + keyId));

        return stringToPublicKey(signingKey.getPublicKey());
    }

    @Override
    @Transactional(readOnly = true)
    public SigningKey getSigningKey(long id) {
        return signingKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Signing key not found for id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SigningKey> getSigningKeys(PlatformType type, long environmentId) {
        return signingKeyRepository.findAllByTypeAndEnvironment(type.ordinal(), (int) environmentId);
    }

    @Override
    public SigningKey update(SigningKey signingKey) {
        Assert.notNull(signingKey, "'signingKey' must not be null");

        SigningKey curSigningKey = getSigningKey(Validate.notNull(signingKey.getId(), "id"));

        curSigningKey.setName(Validate.notNull(signingKey.getName(), "name"));

        return signingKeyRepository.save(curSigningKey);
    }

    @SuppressFBWarnings("VA")
    private static String keyToPrivateString(PrivateKey privateKey) {
        return """
            -----BEGIN PRIVATE KEY-----
            %s
            -----END PRIVATE KEY-----
            """.formatted(MIME_ENCODER.encodeToString(privateKey.getEncoded()));
    }

    private static String keyToPublicString(PublicKey publicKey) {
        return ENCODER.encodeToString(publicKey.getEncoded());
    }

    private static PublicKey stringToPublicKey(String publicKeyString) {
        byte[] publicKeyBytes = DECODER.decode(publicKeyString);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
