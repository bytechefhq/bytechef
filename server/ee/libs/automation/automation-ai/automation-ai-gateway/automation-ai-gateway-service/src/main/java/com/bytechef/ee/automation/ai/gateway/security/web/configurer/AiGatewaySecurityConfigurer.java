/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationProvider;
import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 */
public class AiGatewaySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    protected static final String PATH_PATTERN = "^/api/ai-gateway/v[0-9]+/.+";

    public AiGatewaySecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        super(
            PATH_PATTERN, new AiGatewayApiKeyAuthenticationConverter(),
            new AiGatewayApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private static class AiGatewayApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new AiGatewayApiKeyAuthenticationToken(environment, authToken, tenantId);
        }

    }

}
