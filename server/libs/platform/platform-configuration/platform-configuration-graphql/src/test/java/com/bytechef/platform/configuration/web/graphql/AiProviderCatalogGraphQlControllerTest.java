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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderCatalogFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderCatalogGraphQlControllerTest {

    @Mock
    private AiProviderCatalogFacade aiProviderCatalogFacade;

    @Test
    void testAiProviderCatalogDelegatesToFacade() {
        AiProviderCatalogItemDTO item = new AiProviderCatalogItemDTO(
            "ai.provider.openAi", "Open AI", "<svg/>", true, false,
            List.of(new AiProviderCatalogItemDTO.Model("gpt-5", "GPT-5")));

        when(aiProviderCatalogFacade.getAiProviderCatalog(2)).thenReturn(List.of(item));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderCatalogFacade);

        List<AiProviderCatalogItemDTO> result = controller.aiProviderCatalog(2L);

        assertThat(result).singleElement()
            .extracting(AiProviderCatalogItemDTO::key)
            .isEqualTo("ai.provider.openAi");
    }

    @Test
    void testAiDefaultModelDelegatesToFacade() {
        when(aiProviderCatalogFacade.getAiDefaultModel(2))
            .thenReturn(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));

        AiProviderCatalogGraphQlController controller = new AiProviderCatalogGraphQlController(aiProviderCatalogFacade);

        AiDefaultModelDTO result = controller.aiDefaultModel(2L);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
    }
}
