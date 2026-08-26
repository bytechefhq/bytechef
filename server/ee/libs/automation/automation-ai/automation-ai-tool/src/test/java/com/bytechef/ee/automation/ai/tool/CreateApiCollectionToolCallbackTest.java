/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateApiCollectionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCreateApiCollectionDefaultsCollectionVersionToOne() throws Exception {
        // Pin the default-collectionVersion=1 invariant so the LLM can omit it on a fresh deployment without surprising
        // the facade with a 0-version row. Bumping the contract version is a deliberate user action; the chat agent
        // shouldn't have to know to supply 1 explicitly.
        ApiCollectionFacade facade = mock(ApiCollectionFacade.class);

        ApiCollectionDTO created = new ApiCollectionDTO(
            1, "billing-v1", null, null, null, false, List.of(), Environment.STAGING, 100L, null, null, "Billing API",
            null, 42L, null, 200L, 3, List.of(), 0, null);

        when(facade.createApiCollection(any(ApiCollectionDTO.class))).thenReturn(created);

        CreateApiCollectionToolCallback callback = new CreateApiCollectionToolCallback(facade);

        String result = callback.call(
            "{\"projectId\":\"42\",\"projectVersion\":3,\"environment\":\"STAGING\"," +
                "\"name\":\"Billing API\",\"contextPath\":\"billing-v1\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("apiCollectionId")
            .asLong()).isEqualTo(100L);
        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(200L);
        assertThat(node.get("contextPath")
            .asText()).isEqualTo("billing-v1");
        assertThat(node.get("collectionVersion")
            .asInt()).isEqualTo(1);

        ArgumentCaptor<ApiCollectionDTO> captor = ArgumentCaptor.forClass(ApiCollectionDTO.class);

        verify(facade).createApiCollection(captor.capture());

        ApiCollectionDTO captured = captor.getValue();

        assertThat(captured.collectionVersion()).isEqualTo(1);
        assertThat(captured.environment()).isEqualTo(Environment.STAGING);
        assertThat(captured.projectId()).isEqualTo(42L);
        assertThat(captured.projectVersion()).isEqualTo(3);
        assertThat(captured.contextPath()).isEqualTo("billing-v1");
    }

    @Test
    void testRejectsMissingContextPath() throws Exception {
        CreateApiCollectionToolCallback callback = new CreateApiCollectionToolCallback(
            mock(ApiCollectionFacade.class));

        String result = callback.call(
            "{\"projectId\":\"1\",\"projectVersion\":1,\"environment\":\"DEVELOPMENT\",\"name\":\"x\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("contextPath");
    }

    @Test
    void testCustomCollectionVersionPassedThrough() throws Exception {
        ApiCollectionFacade facade = mock(ApiCollectionFacade.class);

        ApiCollectionDTO created = new ApiCollectionDTO(
            5, "billing-v5", null, null, null, false, List.of(), Environment.PRODUCTION, 100L, null, null, "Billing v5",
            null, 42L, null, 200L, 7, List.of(), 0, null);

        when(facade.createApiCollection(any(ApiCollectionDTO.class))).thenReturn(created);

        CreateApiCollectionToolCallback callback = new CreateApiCollectionToolCallback(facade);

        String result = callback.call(
            "{\"projectId\":\"42\",\"projectVersion\":7,\"environment\":\"PRODUCTION\"," +
                "\"name\":\"Billing v5\",\"contextPath\":\"billing-v5\",\"collectionVersion\":5}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("collectionVersion")
            .asInt()).isEqualTo(5);
    }
}
