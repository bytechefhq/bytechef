/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataAnalystConfigurationTest {

    private final DataAnalystConfiguration dataAnalystConfiguration = new DataAnalystConfiguration();

    @Test
    void testDataAnalystChatClientIsBuilt() {
        ChatModel chatModel = mock(ChatModel.class);
        DataTableService dataTableService = mock(DataTableService.class);
        DataTableRowService dataTableRowService = mock(DataTableRowService.class);
        WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);
        Resource promptResource = new ByteArrayResource(
            "You are a data-analyst subagent.".getBytes(StandardCharsets.UTF_8),
            "test prompt_data_analyst.txt");

        assertThatNoException().isThrownBy(
            () -> dataAnalystConfiguration.dataAnalystChatClient(
                chatModel, dataTableService, dataTableRowService, workspaceDataTableFacade, promptResource));
    }

    @Test
    void testDataAnalystToolCallbackIsNamedCorrectly() {
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = DataAnalystConfiguration.createDataAnalystToolCallback(
            dataAnalystChatClient, assetFileFacade);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("data_analyst");
    }

    @Test
    void testDataAnalystToolCallbackDescriptionMentionsAnalysis() {
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = DataAnalystConfiguration.createDataAnalystToolCallback(
            dataAnalystChatClient, assetFileFacade);

        String description = toolCallback.getToolDefinition()
            .description()
            .toLowerCase();

        assertThat(description).containsAnyOf("analys", "aggregat", "data table");
    }
}
