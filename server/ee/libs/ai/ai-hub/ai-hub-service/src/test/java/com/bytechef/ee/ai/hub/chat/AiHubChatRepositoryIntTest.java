/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiHubChatRepository}. The workspace dimension is the nullable
 * {@code ai_hub_chat.workspace_id} column that the listing queries filter on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubChatRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubChatRepositoryIntTest {

    @Autowired
    private AiHubChatRepository chatRepository;

    @AfterEach
    public void afterEach() {
        chatRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindById() {
        long chatId = saveChatInWorkspace(1L, 10L, "thread-1", AiHubChatStatus.ACTIVE);

        Optional<AiHubChat> found = chatRepository.findById(chatId);

        assertThat(found).isPresent();
        assertThat(found.get()
            .getThreadId()).isEqualTo("thread-1");
        assertThat(found.get()
            .getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);
    }

    @Test
    public void testFindByThreadIdAndUserId() {
        saveChatInWorkspace(1L, 10L, "thread-abc", AiHubChatStatus.ACTIVE);

        Optional<AiHubChat> found = chatRepository.findByThreadIdAndUserId("thread-abc", 10L);

        assertThat(found).isPresent();

        Optional<AiHubChat> notFound = chatRepository.findByThreadIdAndUserId("thread-abc", 99L);

        assertThat(notFound).isEmpty();
    }

    @Test
    public void testFindByWorkspaceIdAndUserIdAndStatusOrderByUpdatedAtDesc() {
        AiHubChat active1 = buildChat(10L, "thread-a1", AiHubChatStatus.ACTIVE);

        active1.setUpdatedAt(LocalDateTime.now()
            .minusMinutes(5));

        AiHubChat active2 = buildChat(10L, "thread-a2", AiHubChatStatus.ACTIVE);

        active2.setUpdatedAt(LocalDateTime.now());

        AiHubChat archived = buildChat(10L, "thread-arch", AiHubChatStatus.ARCHIVED);

        saveInWorkspace(active1, 1L);
        saveInWorkspace(active2, 1L);
        saveInWorkspace(archived, 1L);

        List<AiHubChat> activeChats = chatRepository
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(1L, 10L, 0,
                AiHubChatStatus.ACTIVE.ordinal(),
                100);

        assertThat(activeChats).hasSize(2);
        assertThat(activeChats.get(0)
            .getThreadId()).isEqualTo("thread-a2");
        assertThat(activeChats.get(1)
            .getThreadId()).isEqualTo("thread-a1");
    }

    @Test
    public void testKindColumnDefaultsToStandardForLegacyRows() {
        AiHubChat chat = new AiHubChat();

        chat.setUserId(10L);
        chat.setThreadId("legacy-thread");
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        long chatId = saveInWorkspace(chat, 1L);

        AiHubChat reloaded = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(reloaded.getKind()).isEqualTo(AiHubChatKind.STANDARD);
        assertThat(reloaded.getWorkflowExecutionId()).isNull();
        assertThat(reloaded.getProjectDeploymentId()).isNull();
    }

    @Test
    public void testFindByWorkspaceIdAndUserIdAndStatusFiltersOtherUsers() {
        saveInWorkspace(buildChat(10L, "thread-user10", AiHubChatStatus.ACTIVE), 1L);
        saveInWorkspace(buildChat(20L, "thread-user20", AiHubChatStatus.ACTIVE), 1L);

        List<AiHubChat> chats = chatRepository
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(1L, 10L, 0,
                AiHubChatStatus.ACTIVE.ordinal(),
                100);

        assertThat(chats).hasSize(1);
        assertThat(chats.get(0)
            .getUserId()).isEqualTo(10L);
    }

    @Test
    public void testWorkspaceScopedQueriesIgnoreWorkspaceLessChats() {
        saveInWorkspace(buildChat(10L, "thread-orphan", AiHubChatStatus.ACTIVE), null);

        // SQL equality never matches NULL, so a workspace-less chat is correctly invisible to every workspace query —
        // including workspace 0, which a primitive long field would have collapsed it into.
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(1L, 10L, 0, 100))
            .isEmpty();
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(0L, 10L, 0, 100))
            .isEmpty();
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100)).isEmpty();
    }

    @Test
    public void testInsertAgentChatIfAbsentCreatesTheRow() {
        LocalDateTime now = LocalDateTime.now();

        int inserted = chatRepository.insertAgentChatIfAbsent(
            10L, "slack-C0123", null, AiHubChatStatus.ACTIVE.ordinal(), Environment.PRODUCTION.ordinal(),
            AiHubChatKind.AGENT_CHAT.ordinal(), 1L, 20L, now);

        assertThat(inserted).isEqualTo(1);

        Optional<AiHubChat> found = chatRepository.findByThreadId("slack-C0123");

        assertThat(found).isPresent();

        AiHubChat chat = found.get();

        assertThat(chat.getUserId()).isEqualTo(10L);
        assertThat(chat.getKind()).isEqualTo(AiHubChatKind.AGENT_CHAT);
        assertThat(chat.getAiAgentId()).isEqualTo(20L);
        assertThat(chat.getWorkspaceId()).isEqualTo(1L);
        assertThat(chat.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);
        assertThat(chat.getMessageCount()).isZero();
        assertThat(chat.isAutoTitled()).isTrue();
    }

    @Test
    public void testInsertAgentChatIfAbsentIsANoOpWhenTheThreadIsTaken() {
        LocalDateTime now = LocalDateTime.now();

        chatRepository.insertAgentChatIfAbsent(
            10L, "slack-C0123", null, AiHubChatStatus.ACTIVE.ordinal(), Environment.PRODUCTION.ordinal(),
            AiHubChatKind.AGENT_CHAT.ordinal(), 1L, 20L, now);

        // Both inserts run sequentially on one connection, so this does NOT exercise the cross-transaction blocking
        // a real concurrent-turn race would hit. What it does prove is the property the recorder actually relies on:
        // a taken thread_id yields 0 without raising, leaving the transaction usable — whereas a plain INSERT would
        // raise a constraint violation that aborts it beyond recovery by any later catch.
        int inserted = chatRepository.insertAgentChatIfAbsent(
            99L, "slack-C0123", null, AiHubChatStatus.ACTIVE.ordinal(), Environment.DEVELOPMENT.ordinal(),
            AiHubChatKind.AGENT_CHAT.ordinal(), 2L, 21L, now);

        assertThat(inserted).isZero();

        AiHubChat chat = chatRepository.findByThreadId("slack-C0123")
            .orElseThrow();

        assertThat(chat.getUserId()).isEqualTo(10L);
        assertThat(chat.getAiAgentId()).isEqualTo(20L);
    }

    private long saveChatInWorkspace(
        long workspaceId, long userId, String threadId, AiHubChatStatus status) {
        return saveInWorkspace(buildChat(userId, threadId, status), workspaceId);
    }

    private long saveInWorkspace(AiHubChat chat, @Nullable Long workspaceId) {
        chat.setWorkspaceId(workspaceId);

        AiHubChat saved = chatRepository.save(chat);

        return saved.getId();
    }

    private static AiHubChat buildChat(
        long userId, String threadId, AiHubChatStatus status) {

        AiHubChat chat = new AiHubChat();

        chat.setUserId(userId);
        chat.setThreadId(threadId);
        chat.setStatus(status);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        return chat;
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubChatRepositoryIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
