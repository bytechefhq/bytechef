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
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.SessionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test for the {@link AiHubChatServiceImpl} chat row writes against a real database: two writes of the same
 * chat that overlap in two transactions must both survive, because each writes only its own columns.
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
public class AiHubChatServicePatchIntTest {

    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private AiHubChatServiceImpl chatService;
    private ExecutorService executorService;
    private JdbcTemplate jdbcTemplate;
    private ObjectProvider<AiHubSessionMemory> sessionMemoryProvider;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        sessionMemoryProvider = mock(ObjectProvider.class);

        chatService = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), mock(AiHubToolApprovalService.class),
            sessionMemoryProvider, mock(InFlightAiHubRunRegistry.class));
        executorService = Executors.newFixedThreadPool(2);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    public void afterEach() {
        executorService.shutdownNow();

        chatRepository.deleteAll();
    }

    @Test
    public void testOverlappingPreviewAndTitleUpdatesKeepBothFields() throws Exception {
        long chatId = saveChat();

        runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, "Latest preview", 4, null)),
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch("Generated title", null, null, null)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getTitle()).isEqualTo("Generated title");
        assertThat(chat.isAutoTitled()).isFalse();
        assertThat(chat.getLastPreview()).isEqualTo("Latest preview");
        assertThat(chat.getMessageCount()).isEqualTo(4);
    }

    @Test
    public void testAppendingAnAssistantMessageDuringATitleUpdateKeepsTheTitle() throws Exception {
        long chatId = saveChat();

        AiHubSessionMemory sessionMemory = mock(AiHubSessionMemory.class);

        when(sessionMemory.sessionRepository()).thenReturn(mock(SessionRepository.class));
        when(sessionMemoryProvider.getIfAvailable()).thenReturn(sessionMemory);

        runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch("Generated title", "Latest preview", 4, null)),
            () -> chatService.appendAssistantMessage(chatId, WORKSPACE_ID, USER_ID, "The workflow is ready."));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getTitle()).isEqualTo("Generated title");
        assertThat(chat.isAutoTitled()).isFalse();
        assertThat(chat.getLastPreview()).isEqualTo("Latest preview");
        assertThat(chat.getMessageCount()).isEqualTo(4);
    }

    @Test
    public void testPatchReturnsTheChatWithEveryPatchedField() {
        long chatId = saveChat();

        AiHubChat patchedChat = transactionTemplate.execute(
            transactionStatus -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID,
                new AiHubChatPatch("Renamed", "Preview", 2, AiHubChatStatus.ARCHIVED)));

        assertThat(patchedChat).isNotNull();
        assertThat(patchedChat.getTitle()).isEqualTo("Renamed");
        assertThat(patchedChat.isAutoTitled()).isFalse();
        assertThat(patchedChat.getLastPreview()).isEqualTo("Preview");
        assertThat(patchedChat.getMessageCount()).isEqualTo(2);
        assertThat(patchedChat.getStatus()).isEqualTo(AiHubChatStatus.ARCHIVED);
    }

    @Test
    public void testOverlappingDeleteAndArchiveKeepTheChatDeleted() throws Exception {
        long chatId = saveChat();

        Throwable secondWriteFailure = runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)),
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.ARCHIVED)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
        assertThat(secondWriteFailure).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testTitleGenerationRacingADeleteDoesNotWriteTheTitle() throws Exception {
        long chatId = saveChat();

        Throwable secondWriteFailure = runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)),
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch("Generated title", null, null, null)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
        assertThat(chat.getTitle()).isNull();
        assertThat(chat.isAutoTitled()).isTrue();
        assertThat(secondWriteFailure).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testPreviewPatchOverlappingADeleteDoesNotWriteThePreview() throws Exception {
        long chatId = saveChat();

        Throwable secondWriteFailure = runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)),
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, "Latest preview", null, null)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
        assertThat(chat.getLastPreview()).isNull();
        assertThat(secondWriteFailure).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testMessageCountPatchOverlappingADeleteDoesNotWriteTheCount() throws Exception {
        long chatId = saveChat();

        Throwable secondWriteFailure = runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)),
            () -> chatService.patch(chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, 7, null)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
        assertThat(chat.getMessageCount()).isZero();
        assertThat(secondWriteFailure).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testCombinedPatchOverlappingADeleteWritesNoField() throws Exception {
        long chatId = saveChat();

        Throwable secondWriteFailure = runSecondWhileFirstHoldsTheRow(
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)),
            () -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID,
                new AiHubChatPatch("Renamed", "Latest preview", 3, AiHubChatStatus.ARCHIVED)));

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
        assertThat(chat.getTitle()).isNull();
        assertThat(chat.getLastPreview()).isNull();
        assertThat(chat.getMessageCount()).isZero();
        assertThat(secondWriteFailure).isInstanceOf(ConflictException.class);
    }

    @Test
    public void testRenamingADeletedChatIsRejected() {
        long chatId = saveChat();

        transactionTemplate.executeWithoutResult(
            transactionStatus -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
            transactionStatus -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch("Renamed", null, null, null))))
                    .isInstanceOf(ConflictException.class);

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getTitle()).isNull();
    }

    @Test
    public void testPatchingADeletedChatToDeletedAgainSucceeds() {
        long chatId = saveChat();

        AiHubChatPatch deletePatch = new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED);

        transactionTemplate.executeWithoutResult(
            transactionStatus -> chatService.patch(chatId, WORKSPACE_ID, USER_ID, deletePatch));

        AiHubChat patchedChat = transactionTemplate.execute(
            transactionStatus -> chatService.patch(chatId, WORKSPACE_ID, USER_ID, deletePatch));

        assertThat(patchedChat).isNotNull();
        assertThat(patchedChat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
    }

    @Test
    public void testArchivingADeletedChatIsRejected() {
        long chatId = saveChat();

        transactionTemplate.executeWithoutResult(
            transactionStatus -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
            transactionStatus -> chatService.patch(
                chatId, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.ARCHIVED))))
                    .isInstanceOf(ConflictException.class);

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow();

        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.DELETED);
    }

    private @Nullable Throwable runSecondWhileFirstHoldsTheRow(Runnable firstWrite, Runnable secondWrite)
        throws Exception {

        CountDownLatch firstWrittenLatch = new CountDownLatch(1);
        CountDownLatch releaseFirstLatch = new CountDownLatch(1);

        Future<?> firstFuture = executorService.submit(
            () -> transactionTemplate.executeWithoutResult(transactionStatus -> {
                firstWrite.run();

                firstWrittenLatch.countDown();

                awaitLatch(releaseFirstLatch);
            }));

        assertThat(firstWrittenLatch.await(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();

        Future<?> secondFuture = executorService.submit(
            () -> transactionTemplate.executeWithoutResult(transactionStatus -> secondWrite.run()));

        awaitTransactionBlockedOnRowLock();

        releaseFirstLatch.countDown();

        firstFuture.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

        try {
            secondFuture.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException executionException) {
            return executionException.getCause();
        }

        return null;
    }

    private void awaitTransactionBlockedOnRowLock() throws InterruptedException {
        Instant deadline = Instant.now()
            .plus(WAIT_TIMEOUT);

        while (countSessionsWaitingOnLock() == 0) {
            assertThat(Instant.now()).isBefore(deadline);

            Thread.sleep(20);
        }
    }

    private int countSessionsWaitingOnLock() {
        Integer waitingSessionCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
            Integer.class);

        return waitingSessionCount == null ? 0 : waitingSessionCount;
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            assertThat(latch.await(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private long saveChat() {
        AiHubChat chat = new AiHubChat();

        chat.setUserId(USER_ID);
        chat.setThreadId("thread-patch");
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        chat.setWorkspaceId(WORKSPACE_ID);

        AiHubChat savedChat = chatRepository.save(chat);

        return savedChat.getId();
    }
}
