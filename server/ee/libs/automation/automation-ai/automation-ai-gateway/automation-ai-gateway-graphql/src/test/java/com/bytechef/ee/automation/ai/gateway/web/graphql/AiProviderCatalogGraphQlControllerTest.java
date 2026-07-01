/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderCatalogGraphQlControllerTest {

    @Mock
    private AiProviderFacade aiProviderFacade;

    @Test
    void testAiProviderCatalogDelegatesToFacade() {
        AiProviderCatalogItemDTO item = new AiProviderCatalogItemDTO(
            "ai.provider.openAi", "Open AI", "<svg/>", true, false,
            List.of(new AiProviderCatalogItemDTO.Model("gpt-5", "GPT-5")));

        when(aiProviderFacade.getAiChatProviderCatalog(2)).thenReturn(List.of(item));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderFacade);

        List<AiProviderCatalogItemDTO> result = controller.aiProviderCatalog(2L);

        assertThat(result).singleElement()
            .extracting(AiProviderCatalogItemDTO::key)
            .isEqualTo("ai.provider.openAi");
    }

    @Test
    void testAiDefaultModelDelegatesToFacade() {
        when(aiProviderFacade.getAiDefaultChatModel(2))
            .thenReturn(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderFacade);

        AiDefaultModelDTO result = controller.aiDefaultModel(2L);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
    }
}
