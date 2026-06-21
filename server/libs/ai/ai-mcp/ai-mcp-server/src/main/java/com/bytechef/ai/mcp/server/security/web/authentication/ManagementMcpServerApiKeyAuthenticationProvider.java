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

package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ManagementMcpServerApiKeyAuthenticationToken token =
            (ManagementMcpServerApiKeyAuthenticationToken) authentication;

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        String configuredSecretKey = property == null ? null : (String) property.get("secretKey");
        String providedSecretKey = token.getMcpServerSecretKey();

        if (configuredSecretKey == null || configuredSecretKey.isBlank() || providedSecretKey == null ||
            providedSecretKey.isBlank() ||
            !MessageDigest.isEqual(
                configuredSecretKey.getBytes(StandardCharsets.UTF_8),
                providedSecretKey.getBytes(StandardCharsets.UTF_8))) {

            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        return new ManagementMcpServerApiKeyAuthenticationToken(
            new User("system", "", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ManagementMcpServerApiKeyAuthenticationToken.class);
    }
}
