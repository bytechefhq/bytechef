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

import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
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
    AiProviderCatalogGraphQlController.class
})
@GraphQlTest(
    controllers = AiProviderCatalogGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
class AiProviderCatalogGraphQlControllerIntTest {

    @Autowired
    private AiProviderFacade aiProviderFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAiProviderCatalogDelegatesToFacade() {
        AiProviderCatalogItemDTO aiProviderCatalogItemDTO = new AiProviderCatalogItemDTO(
            "ai.provider.openAi", "Open AI", "<svg/>", true, false,
            List.of(new AiProviderCatalogItemDTO.Model("gpt-5", "GPT-5")));

        when(aiProviderFacade.getAiChatProviderCatalog(2)).thenReturn(List.of(aiProviderCatalogItemDTO));

        graphQlTester
            .document("""
                query {
                    aiProviderCatalog(environment: 2) {
                        key
                        name
                        icon
                        enabled
                        supportsModelById
                        models {
                            name
                            label
                        }
                    }
                }
                """)
            .execute()
            .path("aiProviderCatalog[*].key")
            .entityList(String.class)
            .containsExactly("ai.provider.openAi")
            .path("aiProviderCatalog[0].name")
            .entity(String.class)
            .isEqualTo("Open AI")
            .path("aiProviderCatalog[0].icon")
            .entity(String.class)
            .isEqualTo("<svg/>")
            .path("aiProviderCatalog[0].enabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiProviderCatalog[0].supportsModelById")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiProviderCatalog[0].models[0].name")
            .entity(String.class)
            .isEqualTo("gpt-5")
            .path("aiProviderCatalog[0].models[0].label")
            .entity(String.class)
            .isEqualTo("GPT-5");
    }

    @Test
    void testAiDefaultModelDelegatesToFacade() {
        when(aiProviderFacade.getAiDefaultChatModel(2))
            .thenReturn(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));

        graphQlTester
            .document("""
                query {
                    aiDefaultModel(environment: 2) {
                        provider
                        model
                    }
                }
                """)
            .execute()
            .path("aiDefaultModel.provider")
            .entity(String.class)
            .isEqualTo("ai.provider.anthropic")
            .path("aiDefaultModel.model")
            .entity(String.class)
            .isEqualTo("claude-sonnet-4-6");
    }
}
