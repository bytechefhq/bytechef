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

package com.bytechef.ai.mcp.server.configuration.web.graphql;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing MCP Server configuration. Provides read/write access to platform-level property
 * "mcp.server.key".
 *
 * @author ByteChef
 */
@Controller
class ManagementMcpServerGraphQlController {

    private static final String MCP_SERVER_PROPERTY_KEY = "mcp.server";

    private final PropertyService propertyService;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    ManagementMcpServerGraphQlController(ApplicationProperties applicationProperties, PropertyService propertyService) {
        this.propertyService = propertyService;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @QueryMapping
    String managementMcpServerUrl() {
        Optional<Property> propertyOptional = propertyService.fetchProperty(
            MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null);
        String secretKey;

        if (propertyOptional.isPresent()) {
            Property property = propertyOptional.get();

            secretKey = (String) property.get("secretKey");
        } else {
            secretKey = String.valueOf(TenantKey.of());

            propertyService.save(
                MCP_SERVER_PROPERTY_KEY, Map.of("secretKey", secretKey), Property.Scope.PLATFORM, null);
        }

        return getManagementMcpServerUrl(secretKey);
    }

    @MutationMapping
    String updateManagementMcpServerUrl() {
        String secretKey = String.valueOf(TenantKey.of());

        propertyService.save(MCP_SERVER_PROPERTY_KEY, Map.of("secretKey", secretKey), Property.Scope.PLATFORM, null);

        return getManagementMcpServerUrl(secretKey);
    }

    private String getManagementMcpServerUrl(String secretKey) {
        return publicUrl + "/api/management/" + secretKey + "/mcp";
    }
}
