/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class SigningKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final SigningKeyRepository signingKeyRepository;

    @SuppressFBWarnings("EI")
    public SigningKeyOwnershipResolver(SigningKeyRepository signingKeyRepository) {
        this.signingKeyRepository = signingKeyRepository;
    }

    @Override
    public String resourceType() {
        return "SigningKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return signingKeyRepository.findById(id)
            .filter(signingKey -> signingKey.getType() == PlatformType.EMBEDDED)
            .map(signingKey -> ResourceOwner.ofEmbedded(signingKey.getUserId()))
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}
