/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
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
public class EmbeddedApiKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final ApiKeyService apiKeyService;

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyOwnershipResolver(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public String resourceType() {
        return "ApiKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return apiKeyService.fetchApiKey(id)
            .filter(apiKey -> apiKey.getType() == PlatformType.EMBEDDED)
            .map(EmbeddedApiKeyOwnershipResolver::toOwner)
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }

    private static ResourceOwner toOwner(ApiKey apiKey) {
        Long userId = apiKey.getUserId();

        if (userId == null) {
            return ResourceOwner.ofEmbedded();
        }

        return ResourceOwner.ofEmbedded(userId);
    }
}
