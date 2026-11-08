/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubExecutionIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-execution-testint.xml",
        "spring.liquibase.contexts=mono"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubChatToolRepositoryIntTest {

    @Autowired
    private AiHubChatComponentRepository chatComponentRepository;

    @Autowired
    private AiHubChatConnectorRepository chatConnectorRepository;

    @Autowired
    private AiHubChatRepository chatRepository;

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
    public void testTheMonoContextCreatesEveryForeignKey() {
        List<String> foreignKeyNames = jdbcTemplate.queryForList(
            """
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE constraint_type = 'FOREIGN KEY'
                AND table_name IN (
                    'ai_hub_chat_component', 'ai_hub_chat_tool', 'ai_hub_chat_connector', 'ai_hub_mcp_server',
                    'ai_hub_mcp_server_tool')
                """,
            String.class);

        assertThat(foreignKeyNames).containsExactlyInAnyOrder(
            "fk_ccc_chat", "fk_ccc_connection", "fk_chc_chat", "fk_cct_component", "fk_ahmst_mcp_server");
    }

    @Test
    public void testDeletingAConnectionClearsItFromTheChatComponentsThatPinIt() {
        jdbcTemplate.update("INSERT INTO connection (id) VALUES (42)");

        AiHubChatComponent chatComponent = chatComponent(saveChat("thread-1"), null, null, "slack");

        chatComponent.setConnectionId(42L);

        chatComponent = chatComponentRepository.save(chatComponent);

        jdbcTemplate.update("DELETE FROM connection WHERE id = 42");

        AiHubChatComponent reloadedChatComponent = chatComponentRepository.findById(chatComponent.getId())
            .orElseThrow();

        assertThat(reloadedChatComponent.getConnectionId()).isNull();
    }

    @Test
    public void testChatComponentAndToolRoundTrip() {
        long chatId = saveChat("thread-1");

        AiHubChatComponent chatComponent = chatComponentRepository.save(chatComponent(chatId, null, null, "slack"));

        AiHubChatTool chatTool = new AiHubChatTool(chatComponent.getId(), "sendMessage", Map.of("channel", "#eng"));

        chatTool.setRequiresApproval(true);

        chatToolRepository.save(chatTool);

        AiHubChatTool found = chatToolRepository.findByChatComponentIdAndName(chatComponent.getId(), "sendMessage")
            .orElseThrow();

        assertThat(found.getParameters()).isEqualTo(Map.of("channel", "#eng"));
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.isRequiresApproval()).isTrue();
        assertThat(chatComponentRepository.findAllByChatId(chatId)).hasSize(1);
    }

    @Test
    public void testUserConnectorsAreScopedByUserAndWorkspace() {
        chatComponentRepository.save(chatComponent(null, 10L, 1L, "slack"));
        chatComponentRepository.save(chatComponent(null, 10L, 1L, "github"));
        chatComponentRepository.save(chatComponent(null, 20L, 1L, "slack"));
        chatComponentRepository.save(chatComponent(null, 10L, 2L, "slack"));

        assertThat(chatComponentRepository.findAllByUserIdAndWorkspaceId(10L, 1L))
            .extracting(AiHubChatComponent::getComponentName)
            .containsExactlyInAnyOrder("slack", "github");
        assertThat(
            chatComponentRepository.findByUserIdAndWorkspaceIdAndComponentNameAndComponentVersion(10L, 1L, "slack", 1))
                .isPresent();
    }

    @Test
    public void testAUserCanAddAConnectorOnlyOncePerWorkspace() {
        chatComponentRepository.save(chatComponent(null, 10L, 1L, "slack"));

        assertThatThrownBy(() -> chatComponentRepository.save(chatComponent(null, 10L, 1L, "slack")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testTheUserConnectorUniquenessDoesNotApplyToChatBindings() {
        long chatOneId = saveChat("thread-1");
        long chatTwoId = saveChat("thread-2");

        AiHubChatComponent first = chatComponent(chatOneId, 10L, 1L, "slack");
        AiHubChatComponent second = chatComponent(chatTwoId, 10L, 1L, "slack");

        chatComponentRepository.save(first);
        chatComponentRepository.save(second);

        assertThat(chatComponentRepository.findAll()).hasSize(2);
    }

    @Test
    public void testDeletingAChatCascadesToItsComponentsToolsAndConnectorSwitches() {
        long chatId = saveChat("thread-1");

        AiHubChatComponent chatComponent = chatComponentRepository.save(chatComponent(chatId, null, null, "slack"));

        chatToolRepository.save(new AiHubChatTool(chatComponent.getId(), "sendMessage", Map.of()));
        chatConnectorRepository.save(new AiHubChatConnector(chatId, "github", false));

        chatRepository.deleteById(chatId);

        assertThat(chatComponentRepository.findAll()).isEmpty();
        assertThat(chatToolRepository.findAll()).isEmpty();
        assertThat(chatConnectorRepository.findAll()).isEmpty();
    }

    @Test
    public void testDeletingAComponentCascadesToItsTools() {
        AiHubChatComponent chatComponent = chatComponentRepository.save(chatComponent(null, 10L, 1L, "slack"));

        chatToolRepository.save(new AiHubChatTool(chatComponent.getId(), "sendMessage", Map.of()));
        chatToolRepository.save(new AiHubChatTool(chatComponent.getId(), "listChannels", Map.of()));

        chatComponentRepository.deleteById(chatComponent.getId());

        assertThat(chatToolRepository.findAllByChatComponentId(chatComponent.getId())).isEmpty();
    }

    @Test
    public void testAToolNameIsUniqueWithinItsComponent() {
        AiHubChatComponent chatComponent = chatComponentRepository.save(chatComponent(null, 10L, 1L, "slack"));

        chatToolRepository.save(new AiHubChatTool(chatComponent.getId(), "sendMessage", Map.of()));

        assertThatThrownBy(
            () -> chatToolRepository.save(new AiHubChatTool(chatComponent.getId(), "sendMessage", Map.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
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

        return chatRepository.save(chat)
            .getId();
    }

    private static AiHubChatComponent chatComponent(Long chatId, Long userId, Long workspaceId, String componentName) {
        AiHubChatComponent chatComponent = new AiHubChatComponent();

        chatComponent.setChatId(chatId);
        chatComponent.setUserId(userId);
        chatComponent.setWorkspaceId(workspaceId);
        chatComponent.setComponentName(componentName);
        chatComponent.setComponentVersion(1);
        chatComponent.setEnvironment(0);

        return chatComponent;
    }
}
