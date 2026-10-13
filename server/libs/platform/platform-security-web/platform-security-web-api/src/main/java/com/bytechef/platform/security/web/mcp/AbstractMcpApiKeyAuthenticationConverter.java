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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Builds {@link McpApiKeyCredentials} from the bearer API key, the {@code X-ENVIRONMENT} header and the server secret
 * key carried in the request path.
 *
 * @author Ivica Cardic
 */
public abstract class AbstractMcpApiKeyAuthenticationConverter implements AuthenticationConverter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ENVIRONMENT_HEADER_NAME = "X-ENVIRONMENT";

    protected final String pathPrefix;

    private final EnvironmentService environmentService;

    protected AbstractMcpApiKeyAuthenticationConverter(EnvironmentService environmentService, String pathPrefix) {
        this.environmentService = environmentService;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER_NAME);

        String secretKey = null;

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            secretKey = authorization.substring(BEARER_PREFIX.length());
        }

        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(getEnvironment(request), extractServerSecretKey(request), secretKey));
    }

    protected abstract String extractServerSecretKey(HttpServletRequest request);

    private Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader(ENVIRONMENT_HEADER_NAME);

        try {
            return environmentService.getEnvironment(environment);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid X-ENVIRONMENT header", illegalArgumentException);
        }
    }
}
