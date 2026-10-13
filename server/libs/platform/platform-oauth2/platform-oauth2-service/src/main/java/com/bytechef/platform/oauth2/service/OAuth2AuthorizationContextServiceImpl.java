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

package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.encryption.Encryption;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2AuthorizationContextServiceImpl implements OAuth2AuthorizationContextService {

    static final Duration DISCOVERY_TTL = Duration.ofMinutes(10);
    static final Duration PKCE_TTL = Duration.ofHours(1);

    private static final String AUTHENTICATED_ENCRYPTION_PREFIX = "v2:";
    private static final String COMPONENT_NAME = "componentName";
    private static final String EXPIRES_AT = "expiresAt";
    private static final String PRINCIPAL = "principal";
    private static final String TENANT_ID = "tenantId";

    private final Clock clock;
    private final Encryption encryption;

    @Autowired
    public OAuth2AuthorizationContextServiceImpl(Encryption encryption) {
        this(encryption, Clock.systemUTC());
    }

    OAuth2AuthorizationContextServiceImpl(Encryption encryption, Clock clock) {
        this.encryption = encryption;
        this.clock = clock;
    }

    @Override
    public String seal(OAuth2AuthorizationContext authorizationContext) {
        Duration ttl = authorizationContext.isDiscovery() ? DISCOVERY_TTL : PKCE_TTL;

        Map<String, Object> payload = new HashMap<>();

        payload.put(COMPONENT_NAME, authorizationContext.componentName());
        payload.put("connectionVersion", authorizationContext.connectionVersion());
        payload.put("codeVerifier", authorizationContext.codeVerifier());
        payload.put("clientId", authorizationContext.clientId());
        payload.put("clientSecret", authorizationContext.clientSecret());
        payload.put("tokenEndpointAuthMethod", authorizationContext.tokenEndpointAuthMethod());
        payload.put("authorizationUrl", authorizationContext.authorizationUrl());
        payload.put("tokenUrl", authorizationContext.tokenUrl());
        payload.put("issuer", authorizationContext.issuer());
        payload.put("resource", authorizationContext.resource());
        payload.put(TENANT_ID, TenantContext.getCurrentTenantId());
        payload.put(PRINCIPAL, SecurityUtils.getCurrentUserLogin());
        payload.put(EXPIRES_AT, clock.instant()
            .plus(ttl)
            .getEpochSecond());

        return encryption.encrypt(JsonUtils.write(payload));
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2AuthorizationContext unseal(String sealedAuthorizationContext, String componentName) {
        if (sealedAuthorizationContext == null ||
            !sealedAuthorizationContext.startsWith(AUTHENTICATED_ENCRYPTION_PREFIX)) {

            throw invalid("Sign-in request was altered; try again", null);
        }

        Map<String, Object> payload;
        long expiresAt;

        try {
            payload = JsonUtils.read(encryption.decrypt(sealedAuthorizationContext), Map.class);
            expiresAt = ((Number) payload.get(EXPIRES_AT)).longValue();
        } catch (RuntimeException runtimeException) {
            throw invalid("Sign-in took too long or the request was altered; try again", runtimeException);
        }

        if (expiresAt <= clock.instant()
            .getEpochSecond()) {

            throw invalid("Sign-in took too long; try again", null);
        }

        Optional<String> principal = SecurityUtils.fetchCurrentUserLogin();

        if (principal.isEmpty() || !Objects.equals(payload.get(TENANT_ID), TenantContext.getCurrentTenantId()) ||
            !Objects.equals(payload.get(PRINCIPAL), principal.get()) ||
            !Objects.equals(payload.get(COMPONENT_NAME), componentName)) {

            throw invalid("This sign-in does not belong to the current user or component; try again", null);
        }

        try {
            return new OAuth2AuthorizationContext(
                componentName, ((Number) payload.get("connectionVersion")).intValue(),
                (String) payload.get("codeVerifier"), (String) payload.get("clientId"),
                (String) payload.get("clientSecret"), (String) payload.get("tokenEndpointAuthMethod"),
                (String) payload.get("authorizationUrl"), (String) payload.get("tokenUrl"),
                (String) payload.get("issuer"), (String) payload.get("resource"));
        } catch (RuntimeException runtimeException) {
            throw invalid("Sign-in request was altered; try again", runtimeException);
        }
    }

    private static ConfigurationException invalid(String message, Throwable cause) {
        return cause == null
            ? new ConfigurationException(message, OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT)
            : new ConfigurationException(message, cause, OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT);
    }
}
