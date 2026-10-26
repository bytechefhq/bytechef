/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailTokenSessionRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.ai.sensitivedata.PiiTokenSessionStore;
import com.bytechef.platform.ai.sensitivedata.PiiTokenSessionStore.LoadResult;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EncryptedPiiTokenSessionStoreTest {

    private static final PiiTokenSessionStore.SessionKey KEY =
        new PiiTokenSessionStore.SessionKey(42L, 7L, "thread-1");

    private final Encryption encryption = new Encryption() {

        @Override
        public String encrypt(String content) {
            return "enc:" + new StringBuilder(content).reverse();
        }

        @Override
        public String decrypt(String encryptedString) {
            return new StringBuilder(encryptedString.substring(4)).reverse()
                .toString();
        }
    };

    @Test
    void testWhatIsWrittenToTheRowIsNotThePlainValue() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);
        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        store.save(KEY, Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io"));

        ArgumentCaptor<AiGuardrailTokenSession> captor = ArgumentCaptor.forClass(AiGuardrailTokenSession.class);

        verify(repository).save(captor.capture());

        AiGuardrailTokenSession savedRow = captor.getValue();

        assertThat(savedRow.getTokens()).doesNotContain("bob@acme.io");

        when(repository.findByWorkspaceIdAndUserIdAndConversationId(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId())).thenReturn(Optional.of(savedRow));

        assertThat(store.load(KEY))
            .isEqualTo(LoadResult.of(Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io")));
    }

    @Test
    void testSavingAnEmptyMapStoresNothing() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);
        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        store.save(KEY, Map.of());

        verify(repository, never()).save(any());

        assertThat(store.load(KEY)).isEqualTo(LoadResult.of(Map.of()));
    }

    @Test
    void testSavingAnEmptyMapDeletesAnExistingRow() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);
        AiGuardrailTokenSession existingRow = new AiGuardrailTokenSession(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId(), "abcd", "enc:whatever");

        when(repository.findByWorkspaceIdAndUserIdAndConversationId(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId())).thenReturn(Optional.of(existingRow));

        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        store.save(KEY, Map.of());

        verify(repository).delete(existingRow);
    }

    @Test
    void testASecondSaveReplacesRatherThanAccumulates() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);
        AtomicReference<AiGuardrailTokenSession> currentRow = new AtomicReference<>();
        AtomicInteger rowsSaved = new AtomicInteger();

        when(repository.findByWorkspaceIdAndUserIdAndConversationId(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId()))
                .thenAnswer(invocation -> Optional.ofNullable(currentRow.get()));
        when(repository.save(any())).thenAnswer(invocation -> {
            AiGuardrailTokenSession row = invocation.getArgument(0);

            if (currentRow.get() != row) {
                rowsSaved.incrementAndGet();
            }

            currentRow.set(row);

            return row;
        });

        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        store.save(KEY, Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io"));
        store.save(KEY,
            Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io", "[PII_US_SSN_2_abcd]", "123-45-6789"));

        LoadResult loadResult = store.load(KEY);

        assertThat(loadResult.tokens()).hasSize(2);
        assertThat(rowsSaved.get()).isEqualTo(1);
    }

    @Test
    void testAFailingStoreReportsItselfUnavailableRatherThanThrowing() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);

        when(repository.findByWorkspaceIdAndUserIdAndConversationId(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId()))
                .thenThrow(new RuntimeException("connection refused"));

        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        assertThat(store.load(KEY)).isEqualTo(LoadResult.unavailable());

        assertThatCode(() -> store.save(KEY, Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io")))
            .doesNotThrowAnyException();
    }

    @Test
    void testAFailingStoreEvictsWithoutThrowing() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);

        doThrow(new RuntimeException("connection refused"))
            .when(repository)
            .deleteByWorkspaceIdAndConversationId(KEY.workspaceId(), KEY.conversationId());

        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        assertThatCode(() -> store.evict(KEY.workspaceId(), KEY.conversationId()))
            .doesNotThrowAnyException();
    }

    @Test
    void testAnUndecryptableRowReportsItselfUnavailableRatherThanThrowing() {
        AiGuardrailTokenSessionRepository repository = mock(AiGuardrailTokenSessionRepository.class);
        EncryptedPiiTokenSessionStore store = new EncryptedPiiTokenSessionStore(repository, encryption,
            objectMapper());

        store.save(KEY, Map.of("[PII_EMAIL_ADDRESS_1_abcd]", "bob@acme.io"));

        ArgumentCaptor<AiGuardrailTokenSession> captor = ArgumentCaptor.forClass(AiGuardrailTokenSession.class);

        verify(repository).save(captor.capture());

        AiGuardrailTokenSession savedRow = captor.getValue();

        savedRow.setTokens("bad");

        when(repository.findByWorkspaceIdAndUserIdAndConversationId(
            KEY.workspaceId(), KEY.userId(), KEY.conversationId())).thenReturn(Optional.of(savedRow));

        assertThat(store.load(KEY)).isEqualTo(LoadResult.unavailable());
    }

    private static ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .build();
    }
}
