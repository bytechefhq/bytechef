/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatAssetFileRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-asset-file-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubChatAssetFileServiceIntTest {

    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private AiHubChatAssetFileRepository chatAssetFileRepository;

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private AiHubChatAssetFileService chatAssetFileService;
    private AiHubChatServiceImpl chatService;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        chatAssetFileService = new AiHubChatAssetFileServiceImpl(chatAssetFileRepository);
        chatService = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), mock(AiHubToolApprovalService.class),
            mock(ObjectProvider.class), mock(InFlightAiHubRunRegistry.class), transactionManager,
            mock(ObjectProvider.class), mock(ApplicationEventPublisher.class));
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    public void afterEach() {
        chatRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM asset_file");
    }

    @Test
    public void testRecordRelationshipIsIdempotent() {
        long chatId = saveChat("thread-idempotent");
        long assetFileId = saveAssetFile("notes.md");

        AiHubChatAssetFile first = chatAssetFileService.recordRelationship(
            chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);
        AiHubChatAssetFile second = chatAssetFileService.recordRelationship(
            chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(chatAssetFileService.findByChatId(chatId)).hasSize(1);
        assertThat(chatAssetFileService.findDistinctAssetFileIdsByChatId(chatId)).containsExactly(assetFileId);
    }

    @Test
    public void testRecordRelationshipRejectsAuthored() {
        long chatId = saveChat("thread-authored-rejected");
        long assetFileId = saveAssetFile("report.md");

        assertThatThrownBy(() -> chatAssetFileService.recordRelationship(
            chatId, assetFileId, AiHubChatAssetFileRelationship.AUTHORED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRecordAuthorshipAllowsOneAuthoringChat() {
        long firstChatId = saveChat("thread-author-1");
        long secondChatId = saveChat("thread-author-2");
        long assetFileId = saveAssetFile("generated.md");

        AiHubChatAssetFile authored = chatAssetFileService.recordAuthorship(firstChatId, assetFileId);

        assertThat(chatAssetFileService.recordAuthorship(firstChatId, assetFileId)
            .getId()).isEqualTo(authored.getId());
        assertThat(chatAssetFileService.findAuthoredByAssetFileId(assetFileId)).hasValueSatisfying(
            chatAssetFile -> assertThat(chatAssetFile.getChatId()).isEqualTo(firstChatId));

        assertThatThrownBy(() -> chatAssetFileService.recordAuthorship(secondChatId, assetFileId))
            .isInstanceOf(AuthorshipAlreadyAssignedException.class);
    }

    @Test
    public void testDatabaseRejectsSecondAuthoringChat() {
        long firstChatId = saveChat("thread-index-1");
        long secondChatId = saveChat("thread-index-2");
        long assetFileId = saveAssetFile("generated.md");

        chatAssetFileService.recordAuthorship(firstChatId, assetFileId);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO ai_hub_chat_asset_file (chat_id, asset_file_id, relationship, created_at) VALUES (?, ?, 0, ?)",
            secondChatId, assetFileId, LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testDeleteRelationshipRemovesOnlyThatRelationship() {
        long chatId = saveChat("thread-detach");
        long assetFileId = saveAssetFile("notes.md");

        chatAssetFileService.recordRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);
        chatAssetFileService.recordRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.MENTIONED);

        chatAssetFileService.deleteRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);

        List<AiHubChatAssetFile> remaining = chatAssetFileService.findByChatId(chatId);

        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst()
            .getRelationship()).isEqualTo(AiHubChatAssetFileRelationship.MENTIONED);
    }

    @Test
    public void testDeletingChatRemovesItsRowsButKeepsTheAssetFiles() {
        long deletedChatId = saveChat("thread-deleted");
        long keptChatId = saveChat("thread-kept");
        long assetFileId = saveAssetFile("shared.pdf");

        chatAssetFileService.recordRelationship(deletedChatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);
        chatAssetFileService.recordRelationship(keptChatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);

        chatService.delete(deletedChatId, WORKSPACE_ID, USER_ID);

        assertThat(chatAssetFileService.findByChatId(deletedChatId)).isEmpty();
        assertThat(chatAssetFileService.findByChatId(keptChatId)).hasSize(1);
        assertThat(countAssetFiles(assetFileId)).isEqualTo(1);
    }

    @Test
    public void testDeletingAssetFileRemovesItsRows() {
        long chatId = saveChat("thread-file-deleted");
        long assetFileId = saveAssetFile("gone.csv");

        chatAssetFileService.recordRelationship(chatId, assetFileId, AiHubChatAssetFileRelationship.ATTACHED);

        jdbcTemplate.update("DELETE FROM asset_file WHERE id = ?", assetFileId);

        assertThat(chatAssetFileService.findByChatId(chatId)).isEmpty();
    }

    private int countAssetFiles(long assetFileId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_file WHERE id = ?", Integer.class, assetFileId);

        return count == null ? 0 : count;
    }

    private long saveAssetFile(String name) {
        Long id = jdbcTemplate.queryForObject(
            "INSERT INTO asset_file (name, workspace_id) VALUES (?, ?) RETURNING id", Long.class, name, WORKSPACE_ID);

        assertThat(id).isNotNull();

        return id;
    }

    private long saveChat(String threadId) {
        AiHubChat chat = new AiHubChat();

        chat.setCreatedAt(LocalDateTime.now());
        chat.setMessageCount(0);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setThreadId(threadId);
        chat.setUpdatedAt(LocalDateTime.now());
        chat.setUserId(USER_ID);
        chat.setWorkspaceId(WORKSPACE_ID);

        AiHubChat savedChat = chatRepository.save(chat);

        return savedChat.getId();
    }
}
