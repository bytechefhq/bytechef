/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatArtifactRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link AiHubChatRepository}. The workspace dimension is the nullable
 * {@code ai_hub_chat.workspace_id} column that the listing queries filter on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubChatRepositoryIntTest {

    @Autowired
    private AiHubChatArtifactRepository chatArtifactRepository;

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
        assertThat(found.get()
            .isAutoTitled()).isTrue();
    }

    @Test
    public void testFindByThreadId() {
        saveChatInWorkspace(1L, 10L, "thread-find", AiHubChatStatus.ACTIVE);

        assertThat(chatRepository.findByThreadId("thread-find")).isPresent();
        assertThat(chatRepository.findByThreadId("thread-missing")).isEmpty();
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
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
                1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100);

        assertThat(activeChats).hasSize(2);
        assertThat(activeChats.get(0)
            .getThreadId()).isEqualTo("thread-a2");
        assertThat(activeChats.get(1)
            .getThreadId()).isEqualTo("thread-a1");
    }

    @Test
    public void testArchiveChangesTheListing() {
        long chatId = saveChatInWorkspace(1L, 10L, "thread-to-archive", AiHubChatStatus.ACTIVE);

        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100)).hasSize(1);

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        chat.setStatus(AiHubChatStatus.ARCHIVED);

        chatRepository.save(chat);

        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100)).isEmpty();
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ARCHIVED.ordinal(), 100)).hasSize(1);
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.DELETED.ordinal(), 100)).hasSize(1);
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ARCHIVED.ordinal(), 100)).isEmpty();
    }

    @Test
    public void testFindByWorkspaceIdAndUserIdAndStatusFiltersOtherUsers() {
        saveInWorkspace(buildChat(10L, "thread-user10", AiHubChatStatus.ACTIVE), 1L);
        saveInWorkspace(buildChat(20L, "thread-user20", AiHubChatStatus.ACTIVE), 1L);

        List<AiHubChat> chats = chatRepository
            .findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
                1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100);

        assertThat(chats).hasSize(1);
        assertThat(chats.get(0)
            .getUserId()).isEqualTo(10L);
    }

    @Test
    public void testWorkspaceScopedQueriesIgnoreWorkspaceLessChats() {
        saveInWorkspace(buildChat(10L, "thread-orphan", AiHubChatStatus.ACTIVE), null);

        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.DELETED.ordinal(), 100)).isEmpty();
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
            0L, 10L, 0, AiHubChatStatus.DELETED.ordinal(), 100)).isEmpty();
        assertThat(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            1L, 10L, 0, AiHubChatStatus.ACTIVE.ordinal(), 100)).isEmpty();
    }

    @Test
    public void testDeletingAChatCascadesItsArtifacts() {
        long chatId = saveChatInWorkspace(1L, 10L, "thread-cascade", AiHubChatStatus.ACTIVE);

        AiHubChatArtifact artifact = new AiHubChatArtifact();

        artifact.setChatId(chatId);
        artifact.setKind(AiHubChatArtifactKind.FILE_CREATED);
        artifact.setArtifactId("artifact-1");
        artifact.setArtifactName("report.md");
        artifact.setStatus(AiHubChatArtifactStatus.APPLIED);
        artifact.setEnvironment(Environment.DEVELOPMENT);
        artifact.setCreatedAt(LocalDateTime.now());

        chatArtifactRepository.save(artifact);

        assertThat(chatArtifactRepository.countByChatId(chatId)).isEqualTo(1);

        chatRepository.deleteById(chatId);

        assertThat(chatArtifactRepository.countByChatId(chatId)).isZero();
    }

    private long saveChatInWorkspace(long workspaceId, long userId, String threadId, AiHubChatStatus status) {
        return saveInWorkspace(buildChat(userId, threadId, status), workspaceId);
    }

    private long saveInWorkspace(AiHubChat chat, @Nullable Long workspaceId) {
        chat.setWorkspaceId(workspaceId);

        AiHubChat saved = chatRepository.save(chat);

        return saved.getId();
    }

    private static AiHubChat buildChat(long userId, String threadId, AiHubChatStatus status) {
        AiHubChat chat = new AiHubChat();

        chat.setUserId(userId);
        chat.setThreadId(threadId);
        chat.setStatus(status);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        return chat;
    }
}
