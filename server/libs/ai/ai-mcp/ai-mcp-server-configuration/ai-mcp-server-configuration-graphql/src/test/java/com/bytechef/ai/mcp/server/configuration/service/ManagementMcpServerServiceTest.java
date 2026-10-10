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

package com.bytechef.ai.mcp.server.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerServiceTest {

    private static final String MCP_SERVER_PROPERTY_KEY = "mcp.server";

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final PropertyService propertyService = mock(PropertyService.class);

    private ManagementMcpServerServiceImpl managementMcpServerService;

    @BeforeEach
    void beforeEach() {
        applicationProperties.setPublicUrl("http://localhost:8080");

        managementMcpServerService = new ManagementMcpServerServiceImpl(applicationProperties, propertyService);
    }

    @Test
    void testGetManagementMcpServerUrlFirstTimeSavesOnlyTheSecretKey() {
        when(propertyService.fetchProperty(MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null))
            .thenReturn(Optional.empty());

        String url = managementMcpServerService.getManagementMcpServerUrl();

        verify(propertyService)
            .save(
                eq(MCP_SERVER_PROPERTY_KEY),
                argThat(map -> map.size() == 1 && url.contains("/api/management/" + map.get("secretKey") + "/mcp")),
                eq(Property.Scope.PLATFORM), isNull());
    }

    @Test
    void testGetManagementMcpServerUrlReusesTheStoredSecretKey() {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn("abc");
        when(propertyService.fetchProperty(MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null))
            .thenReturn(Optional.of(property));

        assertThat(managementMcpServerService.getManagementMcpServerUrl())
            .isEqualTo("http://localhost:8080/api/management/abc/mcp");

        verify(propertyService, never()).save(any(), any(), any(), any());
    }

    @Test
    void testUpdateManagementMcpServerUrlRotatesTheSecretKeyAndDropsTheLegacyAuthenticationFlag() {
        String url = managementMcpServerService.updateManagementMcpServerUrl();

        verify(propertyService)
            .save(
                eq(MCP_SERVER_PROPERTY_KEY),
                argThat(map -> map.size() == 1 && !"abc".equals(map.get("secretKey")) &&
                    url.contains("/api/management/" + map.get("secretKey") + "/mcp")),
                eq(Property.Scope.PLATFORM), isNull());
    }
}
