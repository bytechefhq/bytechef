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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderTestResultDTO;
import com.bytechef.platform.ai.provider.facade.WebSearchProviderFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WebSearchProviderGraphQlControllerTest {

    @Mock
    private WebSearchProviderFacade webSearchProviderFacade;

    private WebSearchProviderGraphQlController webSearchProviderGraphQlController;

    @BeforeEach
    void beforeEach() {
        webSearchProviderGraphQlController = new WebSearchProviderGraphQlController(webSearchProviderFacade);
    }

    @Test
    void testWebSearchProvidersDelegatesToTheFacade() {
        WebSearchProviderDTO brave = new WebSearchProviderDTO(
            "brave", "Brave Search", "Web search", null, true, "********1234", false, "https://brave",
            "https://brave");

        when(webSearchProviderFacade.getWebSearchProviders(2)).thenReturn(List.of(brave));

        assertThat(webSearchProviderGraphQlController.webSearchProviders(2L)).containsExactly(brave);
    }

    @Test
    void testTestWebSearchProviderReturnsTheFacadeResult() {
        when(webSearchProviderFacade.testWebSearchProvider("firecrawl", "key", "https://firecrawl.internal", 2))
            .thenReturn(WebSearchProviderTestResultDTO.succeeded());

        assertThat(
            webSearchProviderGraphQlController.testWebSearchProvider(
                "key", "https://firecrawl.internal", 2L, "firecrawl"))
                    .isEqualTo(WebSearchProviderTestResultDTO.succeeded());
    }

    @Test
    void testMutationsDelegateToTheFacade() {
        assertThat(webSearchProviderGraphQlController.updateWebSearchProvider("key", null, 2L, "brave")).isTrue();
        assertThat(webSearchProviderGraphQlController.enableWebSearchProvider(false, 2L, "brave")).isTrue();
        assertThat(webSearchProviderGraphQlController.deleteWebSearchProvider(2L, "firecrawl")).isTrue();

        verify(webSearchProviderFacade).updateWebSearchProvider("brave", "key", null, 2);
        verify(webSearchProviderFacade).enableWebSearchProvider("brave", false, 2);
        verify(webSearchProviderFacade).deleteWebSearchProvider("firecrawl", 2);
    }
}
