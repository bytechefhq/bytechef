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

package com.bytechef.platform.security.web.filter;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApiKeyAuthenticationConverter implements AuthenticationConverter {

    protected static final String AUTH_TOKEN_HEADER_NAME = "Authorization";

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = getAuthToken(request);
        Environment environment = getEnvironment(request);

        TenantKey tenantKey = TenantKey.parse(authToken);

        return doConvert(environment.ordinal(), authToken, tenantKey.getTenantId());
    }

    @Nullable
    protected Authentication doConvert(int environment, String authToken, String tenantId) {
        return null;
    }

    @Nullable
    protected String fetchAuthToken(HttpServletRequest request) {
        String authToken = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (authToken == null) {
            return null;
        }

        return authToken.replace("Bearer ", "");
    }

    protected String getAuthToken(HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (token == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        return token.replace("Bearer ", "");
    }

    protected Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader("X-ENVIRONMENT");

        if (StringUtils.isNotBlank(environment)) {
            return Environment.valueOf(environment.toUpperCase());
        }

        return Environment.PRODUCTION;
    }
}
