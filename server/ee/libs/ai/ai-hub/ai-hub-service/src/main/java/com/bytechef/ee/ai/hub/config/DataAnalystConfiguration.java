/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.ai.hub.tool.DataAnalystToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenDataTableTabToolCallback;
import com.bytechef.ee.ai.hub.tool.QueryDataTableToolCallback;
import com.bytechef.ee.automation.ai.tool.datatable.AggregateDataTableToolCallback;
import com.bytechef.ee.automation.ai.tool.datatable.ListDataTablesToolCallback;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the {@code dataAnalystChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The data_analyst subagent is a dedicated {@link ChatClient} pre-loaded with data-table tools ({@code listDataTables},
 * {@code queryDataTable}, {@code aggregateDataTable}, {@code openDataTableTab}) and the {@code prompt_data_analyst.txt}
 * system prompt. Its isolated context means the parent ai_hub BUILD agent never sees the analysis transcript — it only
 * receives the final markdown report.
 *
 * <p>
 * The {@link DataAnalystToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the
 * ai_hub BUILD agent bean method (via {@link #createDataAnalystToolCallback}) so that it is registered only on that
 * agent. Other agents consume {@code ObjectProvider<ToolCallback>.orderedStream()} and must not receive the
 * data_analyst tool.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class DataAnalystConfiguration {

    @Bean
    ChatClient dataAnalystChatClient(
        ChatModel chatModel,
        DataTableService dataTableService,
        DataTableRowService dataTableRowService,
        WorkspaceDataTableFacade workspaceDataTableFacade,
        @Value("classpath:prompt_data_analyst.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        ListDataTablesToolCallback listDataTables = new ListDataTablesToolCallback(workspaceDataTableFacade);
        QueryDataTableToolCallback queryDataTable =
            new QueryDataTableToolCallback(dataTableRowService, dataTableService);
        AggregateDataTableToolCallback aggregateDataTable =
            new AggregateDataTableToolCallback(dataTableRowService, dataTableService);
        // No server-side artifact recorder: this specialist path relies on the client tab-watching hook to
        // record the reference, same as the ASK mode registration in AiHubConfiguration.
        OpenDataTableTabToolCallback openDataTableTab = new OpenDataTableTabToolCallback(null);

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(listDataTables, queryDataTable, aggregateDataTable, openDataTableTab)
            .build();
    }

    static DataAnalystToolCallback createDataAnalystToolCallback(
        ChatClient dataAnalystChatClient, AssetFileFacade assetFileFacade) {

        return new DataAnalystToolCallback(dataAnalystChatClient, assetFileFacade);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read data analyst prompt resource: " + resource.getDescription(), exception);
        }
    }
}
