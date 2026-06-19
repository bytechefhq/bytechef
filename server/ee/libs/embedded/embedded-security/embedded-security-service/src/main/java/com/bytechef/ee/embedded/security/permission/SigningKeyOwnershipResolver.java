/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.permission;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a signing-key id to its owning user ({@code signing_key.user_id}). Signing keys are user-owned and not
 * workspace-scoped, so authorization is owner-isolation only (enforced in EE; CE has no embedded module). Fails closed
 * when the key does not exist or has no owner.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
public class SigningKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public SigningKeyOwnershipResolver(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    public String resourceType() {
        return "SigningKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return signingKeyService.fetchSigningKey(id)
            .map(SigningKey::getUserId)
            .map(ResourceOwner::ofUser)
            .orElseGet(ResourceOwner::unknown);
    }
}
