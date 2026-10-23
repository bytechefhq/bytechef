/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.OwnedApiKeyCreator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class EmbeddedApiKeyFacadeImpl implements EmbeddedApiKeyFacade {

    private final ApiKeyService apiKeyService;
    private final OwnedApiKeyCreator ownedApiKeyCreator;

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyFacadeImpl(ApiKeyService apiKeyService, OwnedApiKeyCreator ownedApiKeyCreator) {
        this.apiKeyService = apiKeyService;
        this.ownedApiKeyCreator = ownedApiKeyCreator;
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironment('API_KEY_CREATE', #apiKey.environment)")
    public String createApiKey(ApiKey apiKey) {
        ApiKey createdApiKey = ownedApiKeyCreator.create(apiKey, PlatformType.EMBEDDED);

        return createdApiKey.getSecretKey();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiKey', 'API_KEY_DELETE')")
    public void deleteApiKey(long id) {
        getEmbeddedApiKey(id);

        apiKeyService.delete(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApiKey', 'API_KEY_VIEW')")
    public ApiKey getApiKey(long id) {
        return getEmbeddedApiKey(id);
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('API_KEY_VIEW', #environmentId)")
    public List<ApiKey> getApiKeys(long environmentId) {
        return apiKeyService.getApiKeys(environmentId, PlatformType.EMBEDDED);
    }

    @Override
    @PreAuthorize("hasPermission(#apiKey.id, 'ApiKey', 'API_KEY_CREATE')")
    public ApiKey updateApiKey(ApiKey apiKey) {
        getEmbeddedApiKey(apiKey.getId());

        return apiKeyService.update(apiKey);
    }

    private ApiKey getEmbeddedApiKey(long id) {
        ApiKey apiKey = apiKeyService.getApiKey(id);

        if (apiKey.getType() != PlatformType.EMBEDDED) {
            throw new IllegalArgumentException("Api key not found for id: " + id);
        }

        return apiKey;
    }
}
