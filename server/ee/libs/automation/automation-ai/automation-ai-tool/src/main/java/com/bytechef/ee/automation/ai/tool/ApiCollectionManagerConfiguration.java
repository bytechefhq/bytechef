/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.automation.ai.tool.ManagerAgentType;
import com.bytechef.automation.ai.tool.ManagerSubAgentToolCallback;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the {@code apiCollectionManagerChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The api_collection_manager subagent is a dedicated {@link ChatClient} pre-loaded with the API-collection tools
 * ({@code listApiCollections}, {@code createApiCollection}, {@code cloneApiCollection}) and the
 * {@code prompt_api_collection_manager.txt} system prompt. Its isolated context means the parent ai_hub BUILD agent
 * never sees the CRUD transcript — it only receives the final status summary.
 * </p>
 *
 * <p>
 * The {@link ManagerSubAgentToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the
 * ai_hub BUILD agent bean method (via {@link #createApiCollectionManagerToolCallback}) so that it is registered only on
 * that agent — the ASK agent keeps its read-only {@code listApiCollections} flat registration.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ApiCollectionManagerConfiguration {

    static final String TOOL_DESCRIPTION = """
        Delegate API-collection management to a specialised api_collection_manager subagent. API
        collections expose a project version's workflows as REST API endpoints. The subagent lists,
        creates, and clones API collections. Use for requests like "publish these workflows as an API",
        "create an API collection for my CRM project", "clone the orders API into staging". Pass the
        user's request verbatim in 'request' plus any project/collection ids and decisions already
        resolved. The subagent returns a status summary with the affected collection ids; if it reports
        missing decisions (which project, which version, naming), relay them to the user with
        askUserQuestion and re-delegate with the answers.""";

    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    ChatClient apiCollectionManagerChatClient(
        ApiCollectionFacade apiCollectionFacade, ChatModel chatModel,
        @Value("classpath:prompt_api_collection_manager.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(
                new ListApiCollectionsToolCallback(apiCollectionFacade),
                new CreateApiCollectionToolCallback(apiCollectionFacade),
                new CloneApiCollectionToolCallback(apiCollectionFacade))
            .build();
    }

    public static ManagerSubAgentToolCallback createApiCollectionManagerToolCallback(
        ChatClient apiCollectionManagerChatClient) {

        return new ManagerSubAgentToolCallback(
            ManagerAgentType.API_COLLECTION_MANAGER, apiCollectionManagerChatClient, TOOL_DESCRIPTION);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read api collection manager prompt resource: " + resource.getDescription(), exception);
        }
    }
}
