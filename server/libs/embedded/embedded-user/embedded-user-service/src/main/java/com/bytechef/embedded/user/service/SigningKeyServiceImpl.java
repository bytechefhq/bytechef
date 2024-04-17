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

package com.bytechef.embedded.user.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.user.domain.SigningKey;
import com.bytechef.embedded.user.repository.SigningKeyRepository;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    private static KeyPairGenerator keyPairGenerator;

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

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();

        signingKey.setPublicKey(keyToPublicString(publicKey));

        // TODO
        signingKey.setUserId(1050L);

        signingKeyRepository.save(signingKey);

        return keyToPrivateString(keyPair.getPrivate());
    }

    @Override
    public void delete(long id) {
        signingKeyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SigningKey getSigningKey(long id) {
        return OptionalUtils.get(signingKeyRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SigningKey> getSigningKeys() {
        return signingKeyRepository.findAll();
    }

    @Override
    public SigningKey update(@NonNull SigningKey signingKey) {
        Validate.notNull(signingKey, "'signingKey' must not be null");

        SigningKey curSigningKey = getSigningKey(Validate.notNull(signingKey.getId(), "id"));

        curSigningKey.setName(Validate.notNull(signingKey.getName(), "name"));

        return signingKeyRepository.save(curSigningKey);
    }

    private static String keyToPublicString(PublicKey publicKey) {
        byte[] publicKeyBytes = publicKey.getEncoded();

        return ENCODER.encodeToString(publicKeyBytes);
    }

    private static String keyToPrivateString(PrivateKey privateKey) {
        byte[] privateKeyBytes = privateKey.getEncoded();

        return ENCODER.encodeToString(privateKeyBytes);
    }

    private static PublicKey stringToPublicKey(String publicKeyString) throws NoSuchAlgorithmException, Exception {
        byte[] publicKeyBytes = DECODER.decode(publicKeyString);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }
}
