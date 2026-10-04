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
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerSecretVerifier implements McpServerSecretVerifier {

    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("/api/management/(.+)/mcp");

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI2")
    public ManagementMcpServerSecretVerifier(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Optional<Boolean> verifyMcpServerSecret(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        String mcpServerSecretKey = matcher.group(1);

        Optional<Property> property;

        try {
            property = propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null);
        } catch (RuntimeException runtimeException) {
            return Optional.of(false);
        }

        return Optional.of(
            property
                .map(mcpServerProperty -> mcpServerProperty.get("secretKey"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(secretKey -> MessageDigest.isEqual(
                    secretKey.getBytes(StandardCharsets.UTF_8), mcpServerSecretKey.getBytes(StandardCharsets.UTF_8)))
                .orElse(false));
    }
}
