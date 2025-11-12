/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.automation.security.web.authentication.AutomationApiKeyAuthenticationProvider;
import com.bytechef.ee.automation.security.web.authentication.AutomationApiKeyAuthenticationToken;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    protected static final String PATH_PATTERN = "^/api/automation/v[0-9]+/.+";

    public AutomationApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        super(
            PATH_PATTERN, new AutomationApiKeyAuthenticationConverter(),
            new AutomationApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
    }

    private static class AutomationApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new AutomationApiKeyAuthenticationToken(environment, authToken, tenantId);
        }
    }
}
