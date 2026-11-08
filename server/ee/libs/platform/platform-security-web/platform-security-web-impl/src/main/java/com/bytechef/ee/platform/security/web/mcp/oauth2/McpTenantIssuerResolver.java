/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class McpTenantIssuerResolver {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final int CACHE_MAX_ENTRIES = 10_000;

    public enum Surface {

        AUTOMATION(IdentityProvider::isMcpAutomation),
        EMBEDDED(IdentityProvider::isMcpEmbedded),
        MANAGEMENT(IdentityProvider::isMcpManagement);

        private final Predicate<IdentityProvider> appliesTo;

        Surface(Predicate<IdentityProvider> appliesTo) {
            this.appliesTo = appliesTo;
        }

        boolean appliesTo(IdentityProvider identityProvider) {
            return appliesTo.test(identityProvider);
        }
    }

    private final Cache<String, List<McpTenantIssuer>> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .maximumSize(CACHE_MAX_ENTRIES)
        .build();
    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI2")
    public McpTenantIssuerResolver(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    public List<McpTenantIssuer> resolve(Surface surface) {
        String cacheKey = TenantContext.getCurrentTenantId() + ":" + surface;

        return cache.get(cacheKey, unusedKey -> doResolve(surface));
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    @Nullable
    public McpTenantIssuer resolveIssuer(Surface surface, String issuerUri) {
        return resolve(surface).stream()
            .filter(mcpTenantIssuer -> issuerUri.equals(mcpTenantIssuer.issuerUri()))
            .findFirst()
            .orElse(null);
    }

    private List<McpTenantIssuer> doResolve(Surface surface) {
        return identityProviderService.getIdentityProviders()
            .stream()
            .filter(IdentityProvider::isEnabled)
            .filter(surface::appliesTo)
            .filter(identityProvider -> {
                String issuerUri = identityProvider.getIssuerUri();

                return issuerUri != null && !issuerUri.isBlank();
            })
            .map(
                identityProvider -> new McpTenantIssuer(
                    identityProvider.getIssuerUri(), identityProvider.isValidateMcpAudience(),
                    identityProvider.getAuthoritiesClaim(), identityProvider.getAuthorityMappings()))
            .toList();
    }
}
