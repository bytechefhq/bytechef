/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.service;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.platform.constant.PlatformType;
import java.security.PublicKey;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface SigningKeyService {

    String create(SigningKey signingKey);

    void delete(long id);

    PublicKey getPublicKey(String keyId, long environmentId);

    SigningKey getSigningKey(long id);

    List<SigningKey> getSigningKeys(PlatformType type, long environmentId);

    SigningKey update(SigningKey signingKey);
}
