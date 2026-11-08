/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatComponentRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatConnectorRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatToolRepository;
import com.bytechef.ee.ai.hub.config.AiHubExecutionIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubExecutionIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true",
        "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-execution-testint.xml",
        "spring.liquibase.contexts=execution"
    })
@Import({
    AiHubChatToolFacadeImpl.class, PostgreSQLContainerConfiguration.class
})
public class AiHubChatToolFacadeIntTest {

    @Autowired
    private AiHubChatComponentRepository chatComponentRepository;

    @Autowired
    private AiHubChatConnectorRepository chatConnectorRepository;

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private AiHubChatToolFacade chatToolFacade;

    @Autowired
    private AiHubChatToolRepository chatToolRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void afterEach() {
        chatToolRepository.deleteAll();
        chatConnectorRepository.deleteAll();
        chatComponentRepository.deleteAll();
        chatRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM connection");
    }

    @Test
    public void testTheExecutionContextCreatesNoForeignKeyToATableOfAnotherContext() {
        assertThat(foreignKeyNames()).containsExactlyInAnyOrder("fk_cct_component", "fk_ahmst_mcp_server");
    }

    @Test
    public void testDeleteChatBindingsRemovesTheChatsComponentsToolsAndConnectorSwitchesWithoutACascade() {
        long chatId = saveChat("thread-1");
        long otherChatId = saveChat("thread-2");

        long chatComponentId = chatToolFacade.attachComponent(chatId, "slack", 1, null, 0);

        chatToolFacade.addTool(chatComponentId, "sendMessage", Map.of("channel", "#eng"));

        long otherChatComponentId = chatToolFacade.attachComponent(otherChatId, "slack", 1, null, 0);

        chatToolFacade.addTool(otherChatComponentId, "sendMessage", Map.of());

        long userConnectorId = chatToolFacade.attachUserComponent(10L, 1L, "github", 1, null, 0);

        chatToolFacade.setChatConnectorEnabled(chatId, userConnectorId, false);

        chatRepository.deleteById(chatId);

        assertThat(chatComponentRepository.findAllByChatId(chatId)).hasSize(1);
        assertThat(chatConnectorRepository.findAllByChatId(chatId)).hasSize(1);

        chatToolFacade.deleteChatBindings(chatId);

        assertThat(chatComponentRepository.findAllByChatId(chatId)).isEmpty();
        assertThat(chatToolRepository.findAllByChatComponentId(chatComponentId)).isEmpty();
        assertThat(chatConnectorRepository.findAllByChatId(chatId)).isEmpty();
        assertThat(chatToolFacade.listChatTools(otherChatId)).hasSize(1);
        assertThat(chatToolFacade.listUserComponents(10L, 1L))
            .extracting(AiHubChatComponent::getId)
            .containsExactly(userConnectorId);
    }

    @Test
    public void testADeletedConnectionLeavesTheBindingResolvableWithItsPinnedConnectionId() {
        jdbcTemplate.update("INSERT INTO connection (id) VALUES (42)");

        long chatId = saveChat("thread-1");

        long chatComponentId = chatToolFacade.attachComponent(chatId, "slack", 1, 42L, 0);

        chatToolFacade.addTool(chatComponentId, "sendMessage", Map.of());

        jdbcTemplate.update("DELETE FROM connection WHERE id = 42");

        List<AiHubChatToolBinding> chatToolBindings = chatToolFacade.listChatTools(chatId);

        assertThat(chatToolBindings).hasSize(1);
        assertThat(chatToolBindings.getFirst()
            .connectionId()).isEqualTo(42L);
    }

    private List<String> foreignKeyNames() {
        return jdbcTemplate.queryForList(
            """
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE constraint_type = 'FOREIGN KEY'
                AND table_name IN (
                    'ai_hub_chat_component', 'ai_hub_chat_tool', 'ai_hub_chat_connector', 'ai_hub_mcp_server',
                    'ai_hub_mcp_server_tool')
                """,
            String.class);
    }

    private long saveChat(String threadId) {
        AiHubChat chat = new AiHubChat();

        chat.setUserId(10L);
        chat.setWorkspaceId(1L);
        chat.setThreadId(threadId);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        AiHubChat savedChat = chatRepository.save(chat);

        return savedChat.getId();
    }
}
