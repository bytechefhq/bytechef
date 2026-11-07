/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.DataAnalystToolCallback;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
public class DataAnalystConfiguration {

    @Bean
    AiHubSubAgentToolCallbackFactory dataAnalystToolCallbackFactory(
        ChatModel chatModel, ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        WorkspaceDataTableFacade workspaceDataTableFacade, AssetFileFacade assetFileFacade,
        AiHubChatArtifactRecorder chatArtifactRecorder,
        @Value("classpath:prompt_data_analyst.txt") Resource promptResource) {

        IntelligentToolChatClientFactory chatClientFactory = SubAgentConfigurationSupport.chatClientFactory(
            chatModel, SubAgentConfigurationSupport.readPrompt(promptResource, "data analyst"));
        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();
        List<ToolCallback> dataTableToolCallbacks = dataTableToolCallbacks(workspaceDataTableFacade);

        return () -> new DataAnalystToolCallback(
            chatClientFactory, chatModelResolver, dataTableToolCallbacks, assetFileFacade, chatArtifactRecorder);
    }

    static List<ToolCallback> dataTableToolCallbacks(WorkspaceDataTableFacade workspaceDataTableFacade) {
        DataTableToolCallbacksFactory dataTableToolCallbacksFactory =
            new DataTableToolCallbacksFactory(workspaceDataTableFacade);

        return dataTableToolCallbacksFactory.readToolCallbacks();
    }
}
