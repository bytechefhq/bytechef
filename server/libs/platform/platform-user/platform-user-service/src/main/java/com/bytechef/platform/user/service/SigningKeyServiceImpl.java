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

package com.bytechef.platform.user.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.user.domain.SigningKey;
import com.bytechef.platform.user.domain.SigningKey.TenantKeyId;
import com.bytechef.platform.user.repository.SigningKeyRepository;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public String create(@NonNull SigningKey signingKey) {
        Validate.notNull(signingKey, "'signingKey' must not be null");
        Validate.isTrue(signingKey.getId() == null, "'id' must be null");
        Validate.notNull(signingKey.getName(), "'name' must not be null");
        Validate.notNull(signingKey.getType(), "'type' must not be null");

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        signingKey.setKeyId(String.valueOf(TenantKeyId.of()));

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
    public PublicKey getPublicKey(long id) {
        SigningKey signingKey = OptionalUtils.get(signingKeyRepository.findById(id));

        return stringToPublicKey(signingKey.getPublicKey());
    }

    @Override
    public PublicKey getPublicKey(String keyId, Environment environment) {
        SigningKey signingKey = OptionalUtils.get(signingKeyRepository.findByKeyIdAndEnvironment(
            keyId, environment.ordinal()));

        return stringToPublicKey(signingKey.getPublicKey());
    }

    @Override
    @Transactional(readOnly = true)
    public SigningKey getSigningKey(long id) {
        return OptionalUtils.get(signingKeyRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SigningKey> getSigningKeys(AppType type) {
        return signingKeyRepository.findAllByType(type.ordinal());
    }

    @Override
    public SigningKey update(@NonNull SigningKey signingKey) {
        Validate.notNull(signingKey, "'signingKey' must not be null");

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
