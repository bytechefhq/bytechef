/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.tool.CloneAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.CreateAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.DeleteAiHubPersonalAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.ListAiHubPersonalAgentsToolCallback;
import com.bytechef.ee.ai.hub.tool.ManagerSubAgentToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenAiHubPersonalAgentTabToolCallback;
import com.bytechef.ee.ai.hub.tool.UpdateAiHubPersonalAgentToolCallback;
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
 * Registers the {@code personalAgentManagerChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The personal_agent_manager subagent is a dedicated {@link ChatClient} pre-loaded with the personal-agent CRUD tools
 * ({@code listAiHubPersonalAgents}, {@code createAiHubPersonalAgent}, {@code updateAiHubPersonalAgent},
 * {@code deleteAiHubPersonalAgent}, {@code cloneAiHubPersonalAgent}, {@code openAiHubPersonalAgentTab}) and the
 * {@code prompt_personal_agent_manager.txt} system prompt. Its isolated context means the parent ai_hub BUILD agent
 * never sees the CRUD transcript — it only receives the final status summary.
 * </p>
 *
 * <p>
 * The {@link ManagerSubAgentToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in
 * the ai_hub BUILD agent bean method (via {@link #createPersonalAgentManagerToolCallback}) so that it is registered
 * only on that agent — the ASK agent keeps its own read-only flat registrations.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class PersonalAgentManagerConfiguration {

    static final String TOOL_DESCRIPTION = """
        Delegate personal-agent management to a specialised personal_agent_manager subagent. Personal
        agents are the user's named AI assistants with their own instructions overlay. The subagent lists,
        creates, updates (title/instructions), deletes, and clones personal agents, and opens the agent
        tab so the user sees the result. Use for requests like "create an agent that reviews my PRs",
        "rename my Support agent", "duplicate this agent for the sales team". Pass the user's request
        verbatim in 'request' plus any agent ids or decisions already resolved. Deleting is destructive —
        only delegate a delete after the user has explicitly confirmed it. The subagent returns a status
        summary; if it reports missing decisions, relay them to the user and re-delegate with the
        answers.""";

    @Bean
    @ConditionalOnBean(AiHubPersonalAgentService.class)
    ChatClient personalAgentManagerChatClient(
        AiHubPersonalAgentService aiHubPersonalAgentService, AiHubTaskService aiHubTaskService, ChatModel chatModel,
        @Value("classpath:prompt_personal_agent_manager.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(
                new ListAiHubPersonalAgentsToolCallback(aiHubPersonalAgentService),
                new CreateAiHubPersonalAgentToolCallback(aiHubPersonalAgentService),
                new UpdateAiHubPersonalAgentToolCallback(aiHubPersonalAgentService),
                new DeleteAiHubPersonalAgentToolCallback(aiHubPersonalAgentService),
                new CloneAiHubPersonalAgentToolCallback(aiHubPersonalAgentService),
                // No server-side artifact recorder: this specialist path relies on the client tab-watching hook to
                // record the reference, same as the ASK mode registration in AiHubConfiguration.
                new OpenAiHubPersonalAgentTabToolCallback(aiHubPersonalAgentService, aiHubTaskService))
            .build();
    }

    static ManagerSubAgentToolCallback createPersonalAgentManagerToolCallback(
        ChatClient personalAgentManagerChatClient) {

        return new ManagerSubAgentToolCallback(
            AiHubAgentType.PERSONAL_AGENT_MANAGER, personalAgentManagerChatClient, TOOL_DESCRIPTION);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read personal agent manager prompt resource: " + resource.getDescription(), exception);
        }
    }
}
