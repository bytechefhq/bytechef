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

package com.bytechef.platform.configuration.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.dto.McpServerCatalogAuthenticationDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.facade.McpServerCatalogFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class,
    McpServerCatalogGraphQlController.class
})
@GraphQlTest(
    controllers = McpServerCatalogGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
class McpServerCatalogGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpServerCatalogFacade mcpServerCatalogFacade;

    @Test
    void testMcpServerCatalogReturnsEntriesWithAuthentication() {
        when(mcpServerCatalogFacade.getMcpServerCatalog()).thenReturn(List.of(
            new McpServerCatalogEntryDTO(
                "notion", "Notion", "Official Notion MCP server", "https://mcp.notion.com/mcp", null,
                "httpStreamable",
                new McpServerCatalogAuthenticationDTO("oauth2", true, null, null, null, null, null, null),
                "https://developers.notion.com/docs/mcp", "notion")));

        graphQlTester.document("""
            query {
                mcpServerCatalog {
                    slug
                    title
                    url
                    transportType
                    authentication {
                        type
                        dynamicClientRegistration
                    }
                    component
                }
            }
            """)
            .execute()
            .path("mcpServerCatalog[0].slug")
            .entity(String.class)
            .isEqualTo("notion")
            .path("mcpServerCatalog[0].authentication.type")
            .entity(String.class)
            .isEqualTo("oauth2")
            .path("mcpServerCatalog[0].authentication.dynamicClientRegistration")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
}
