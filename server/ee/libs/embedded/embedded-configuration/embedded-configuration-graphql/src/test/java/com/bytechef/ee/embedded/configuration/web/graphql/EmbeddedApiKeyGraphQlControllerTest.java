/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.web.graphql.EmbeddedApiKeyGraphQlController.CreateEmbeddedApiKeyInput;
import com.bytechef.ee.embedded.configuration.web.graphql.EmbeddedApiKeyGraphQlController.UpdateEmbeddedApiKeyInput;
import com.bytechef.ee.embedded.security.facade.EmbeddedApiKeyFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ApiKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyGraphQlControllerTest {

    private final EmbeddedApiKeyFacade embeddedApiKeyFacade = mock(EmbeddedApiKeyFacade.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final EmbeddedApiKeyGraphQlController controller =
        new EmbeddedApiKeyGraphQlController(embeddedApiKeyFacade, environmentService);

    @Test
    void testCreateEmbeddedApiKeyResolvesTheEnvironmentAndReturnsTheSecret() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.PRODUCTION);
        when(embeddedApiKeyFacade.createApiKey(any(ApiKey.class))).thenReturn("secret");

        String secret = controller.createEmbeddedApiKey(new CreateEmbeddedApiKeyInput("my key", 1L));

        assertThat(secret).isEqualTo("secret");

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);

        verify(embeddedApiKeyFacade).createApiKey(apiKeyCaptor.capture());

        ApiKey apiKey = apiKeyCaptor.getValue();

        assertThat(apiKey.getName()).isEqualTo("my key");
        assertThat(apiKey.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(apiKey.getType()).isNull();
        assertThat(apiKey.getUserId()).isNull();
    }

    @Test
    void testUpdateEmbeddedApiKeyPassesOnlyTheIdAndTheName() {
        ApiKey updatedApiKey = new ApiKey();

        updatedApiKey.setId(7L);
        updatedApiKey.setName("renamed");

        when(embeddedApiKeyFacade.updateApiKey(any(ApiKey.class))).thenReturn(updatedApiKey);

        ApiKey result = controller.updateEmbeddedApiKey(new UpdateEmbeddedApiKeyInput(7L, "renamed"));

        assertThat(result).isSameAs(updatedApiKey);

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);

        verify(embeddedApiKeyFacade).updateApiKey(apiKeyCaptor.capture());

        assertThat(apiKeyCaptor.getValue()
            .getId()).isEqualTo(7L);
        assertThat(apiKeyCaptor.getValue()
            .getName()).isEqualTo("renamed");
    }

    @Test
    void testDeleteAndTheReadsDelegateToTheFacade() {
        ApiKey apiKey = new ApiKey();

        when(embeddedApiKeyFacade.getApiKey(3L)).thenReturn(apiKey);
        when(embeddedApiKeyFacade.getApiKeys(1L)).thenReturn(List.of(apiKey));

        assertThat(controller.deleteEmbeddedApiKey(3L)).isTrue();
        assertThat(controller.embeddedApiKey(3L)).isSameAs(apiKey);
        assertThat(controller.embeddedApiKeys(1L)).containsExactly(apiKey);

        verify(embeddedApiKeyFacade).deleteApiKey(3L);
    }
}
