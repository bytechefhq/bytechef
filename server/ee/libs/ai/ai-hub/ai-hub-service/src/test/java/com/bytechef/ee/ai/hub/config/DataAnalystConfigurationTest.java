/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataAnalystConfigurationTest {

    private final DataAnalystConfiguration dataAnalystConfiguration = new DataAnalystConfiguration();

    @Test
    void testDataAnalystToolCallbackFactoryCreatesTheDataAnalystTool() {
        AiHubSubAgentToolCallbackFactory factory = newFactory(new ClassPathResource("prompt_data_analyst.txt"));

        assertThat(factory.create()
            .getToolDefinition()
            .name()).isEqualTo("data_analyst");
    }

    @Test
    void testDataAnalystSubagentOnlyReadsDataTables() {
        assertThat(DataAnalystConfiguration.dataTableToolCallbacks(mock(WorkspaceDataTableFacade.class)))
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder("listDataTables", "queryDataTable", "aggregateDataTable");
    }

    @Test
    void testDataAnalystToolCallbackFactoryFailsOnAMissingPrompt() {
        assertThatThrownBy(() -> newFactory(new ClassPathResource("missing_prompt_data_analyst.txt")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("data analyst prompt");
    }

    @Test
    void testShippedDataAnalystPromptNamesOnlyTheReadTools() throws Exception {
        Resource resource = new ClassPathResource("prompt_data_analyst.txt");

        String prompt = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains("listDataTables", "queryDataTable", "aggregateDataTable");
        assertThat(prompt).doesNotContain("openDataTableTab", "addDataTableRow", "createAssetFile");
    }

    @SuppressWarnings("unchecked")
    private AiHubSubAgentToolCallbackFactory newFactory(Resource promptResource) {
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider = mock(ObjectProvider.class);

        return dataAnalystConfiguration.dataAnalystToolCallbackFactory(
            mock(ChatModel.class), chatModelResolverProvider, mock(WorkspaceDataTableFacade.class),
            mock(AssetFileFacade.class), mock(AiHubChatArtifactRecorder.class), promptResource);
    }
}
