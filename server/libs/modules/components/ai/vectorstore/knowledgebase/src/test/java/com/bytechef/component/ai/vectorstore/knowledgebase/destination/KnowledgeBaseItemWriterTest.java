/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.vectorstore.knowledgebase.destination;

import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE_FULL_REPLACE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE_KEY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.MODE_PARTIAL;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.SEEN_RECORD_IDS_KEY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.destination.KnowledgeBaseItemWriter.SOURCE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.PayloadHashUtil;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link KnowledgeBaseItemWriter}. Mocks the source service + document service and exercises the
 * open/write/update lifecycle for new, unchanged, changed, and reappeared-tombstoned records, the seenRecordIds + mode
 * handoff to the listener, the missing-id guard, and the mode-validation on {@code open()}.
 *
 * @author Ivica Cardic
 */
class KnowledgeBaseItemWriterTest {

    private static final Long SOURCE_ID_VALUE = 200L;
    private static final Long KB_ID_VALUE = 300L;

    private KnowledgeBaseSourceService knowledgeBaseSourceService;
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    private KnowledgeBaseItemWriter writer;
    private Parameters inputParameters;
    private Parameters connectionParameters;
    private Context context;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        knowledgeBaseSourceService = mock(KnowledgeBaseSourceService.class);
        knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);

        writer = new KnowledgeBaseItemWriter(knowledgeBaseSourceService, knowledgeBaseDocumentService);

        inputParameters = mock(Parameters.class);
        connectionParameters = mock(Parameters.class);
        context = mock(Context.class);
        executionContext = mock(ExecutionContext.class);

        when(inputParameters.getRequiredLong(SOURCE_ID)).thenReturn(SOURCE_ID_VALUE);
        when(inputParameters.getString(MODE, MODE_FULL_REPLACE)).thenReturn(MODE_FULL_REPLACE);

        KnowledgeBaseSource source = newSource(KB_ID_VALUE);

        when(knowledgeBaseSourceService.fetch(SOURCE_ID_VALUE)).thenReturn(Optional.of(source));
    }

    @Test
    void testWriteCreatesNewDocumentForFirstSourceRecord() {
        when(knowledgeBaseDocumentService.findSyncedDocument(eq(SOURCE_ID_VALUE), anyString()))
            .thenReturn(Optional.empty());

        writer.open(inputParameters, connectionParameters, context, executionContext);

        Map<String, Object> sourceRecord = new LinkedHashMap<>();

        sourceRecord.put("id", "rec1");
        sourceRecord.put("name", "Document One");
        sourceRecord.put("text", "Hello world");

        writer.write(List.of(sourceRecord));

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);

        verify(knowledgeBaseDocumentService, times(1))
            .createSyncedDocument(
                eq(KB_ID_VALUE), eq(SOURCE_ID_VALUE), eq("rec1"),
                nameCaptor.capture(), textCaptor.capture(), any(), any(), hashCaptor.capture(), any(Instant.class));

        assertThat(nameCaptor.getValue()).isEqualTo("Document One");
        assertThat(textCaptor.getValue()).isEqualTo("Hello world");
        assertThat(hashCaptor.getValue()).isEqualTo(PayloadHashUtil.hash(sourceRecord));

        verify(knowledgeBaseDocumentService, never()).replaceSyncedDocument(
            anyLong(), anyString(), anyString(), any(), any(), anyString(), any(Instant.class));
        verify(knowledgeBaseDocumentService, never()).bumpLastSeenAt(any(), any(Instant.class));
    }

    @Test
    void testWriteUnchangedRecordTakesFastPath() {
        Map<String, Object> sourceRecord = new LinkedHashMap<>();

        sourceRecord.put("id", "rec1");
        sourceRecord.put("name", "Document One");
        sourceRecord.put("text", "Hello world");

        String hash = PayloadHashUtil.hash(sourceRecord);

        KnowledgeBaseDocument existing = newDocument(42L, hash, /* deletedAt */ null);

        when(knowledgeBaseDocumentService.findSyncedDocument(SOURCE_ID_VALUE, "rec1"))
            .thenReturn(Optional.of(existing));

        writer.open(inputParameters, connectionParameters, context, executionContext);

        writer.write(List.of(sourceRecord));

        verify(knowledgeBaseDocumentService, times(1)).bumpLastSeenAt(eq(existing), any(Instant.class));
        verify(knowledgeBaseDocumentService, never()).createSyncedDocument(
            anyLong(), anyLong(), anyString(), anyString(), anyString(), any(), any(), anyString(),
            any(Instant.class));
        verify(knowledgeBaseDocumentService, never()).replaceSyncedDocument(
            anyLong(), anyString(), anyString(), any(), any(), anyString(), any(Instant.class));
    }

    @Test
    void testWriteChangedRecordCallsReplace() {
        Map<String, Object> sourceRecord = new LinkedHashMap<>();

        sourceRecord.put("id", "rec1");
        sourceRecord.put("name", "Document One Updated");
        sourceRecord.put("text", "Hello world updated");

        KnowledgeBaseDocument existing = newDocument(42L, "stale-hash-0000", /* deletedAt */ null);

        when(knowledgeBaseDocumentService.findSyncedDocument(SOURCE_ID_VALUE, "rec1"))
            .thenReturn(Optional.of(existing));

        writer.open(inputParameters, connectionParameters, context, executionContext);

        writer.write(List.of(sourceRecord));

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);

        verify(knowledgeBaseDocumentService, times(1))
            .replaceSyncedDocument(
                eq(42L), nameCaptor.capture(), textCaptor.capture(), any(), any(), hashCaptor.capture(),
                any(Instant.class));

        assertThat(nameCaptor.getValue()).isEqualTo("Document One Updated");
        assertThat(textCaptor.getValue()).isEqualTo("Hello world updated");
        assertThat(hashCaptor.getValue()).isEqualTo(PayloadHashUtil.hash(sourceRecord));

        verify(knowledgeBaseDocumentService, never()).bumpLastSeenAt(any(), any(Instant.class));
        verify(knowledgeBaseDocumentService, never()).createSyncedDocument(
            anyLong(), anyLong(), anyString(), anyString(), anyString(), any(), any(), anyString(),
            any(Instant.class));
    }

    @Test
    void testWriteReappearedTombstonedRecordCallsReplaceClearingDeletedAt() {
        Map<String, Object> sourceRecord = new LinkedHashMap<>();

        sourceRecord.put("id", "rec1");
        sourceRecord.put("name", "Document One");
        sourceRecord.put("text", "Hello world");

        // Same hash as the new payload; but the row was previously tombstoned ⇒ must NOT take the fast path,
        // must call replaceSyncedDocument so deletedAt gets cleared and the chunker pipeline re-runs.
        String hash = PayloadHashUtil.hash(sourceRecord);

        KnowledgeBaseDocument existing = newDocument(42L, hash, Instant.parse("2026-04-01T00:00:00Z"));

        when(knowledgeBaseDocumentService.findSyncedDocument(SOURCE_ID_VALUE, "rec1"))
            .thenReturn(Optional.of(existing));

        writer.open(inputParameters, connectionParameters, context, executionContext);

        writer.write(List.of(sourceRecord));

        verify(knowledgeBaseDocumentService, times(1))
            .replaceSyncedDocument(
                eq(42L), anyString(), anyString(), any(), any(), eq(hash), any(Instant.class));
        verify(knowledgeBaseDocumentService, never()).bumpLastSeenAt(any(), any(Instant.class));
    }

    @Test
    void testWriteMissingIdFieldThrows() {
        when(knowledgeBaseDocumentService.findSyncedDocument(eq(SOURCE_ID_VALUE), anyString()))
            .thenReturn(Optional.empty());

        writer.open(inputParameters, connectionParameters, context, executionContext);

        Map<String, Object> badRecord = new HashMap<>();

        badRecord.put("name", "no-id-here");
        badRecord.put("text", "body");

        assertThatThrownBy(() -> writer.write(List.of(badRecord)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Record missing required 'id' field");
    }

    @Test
    void testUpdateFlushesSeenRecordIdsAndModeToExecutionContext() {
        when(knowledgeBaseDocumentService.findSyncedDocument(eq(SOURCE_ID_VALUE), anyString()))
            .thenReturn(Optional.empty());

        writer.open(inputParameters, connectionParameters, context, executionContext);

        writer.write(List.of(
            Map.of("id", "rec1", "name", "n1", "text", "t1"),
            Map.of("id", "rec2", "name", "n2", "text", "t2"),
            Map.of("id", "rec3", "name", "n3", "text", "t3")));

        writer.update(inputParameters, connectionParameters, context, executionContext);

        ArgumentCaptor<Object> seenCaptor = ArgumentCaptor.forClass(Object.class);

        verify(executionContext).put(eq(SEEN_RECORD_IDS_KEY), seenCaptor.capture());
        verify(executionContext).put(MODE_KEY, MODE_FULL_REPLACE);

        @SuppressWarnings("unchecked")
        List<String> capturedSeenIds = (List<String>) seenCaptor.getValue();

        assertThat(capturedSeenIds).containsExactlyInAnyOrder("rec1", "rec2", "rec3");
    }

    @Test
    void testOpenWithExplicitPartialModeFlushesPartialOnUpdate() {
        when(inputParameters.getString(MODE, MODE_FULL_REPLACE)).thenReturn(MODE_PARTIAL);

        writer.open(inputParameters, connectionParameters, context, executionContext);

        writer.update(inputParameters, connectionParameters, context, executionContext);

        verify(executionContext).put(MODE_KEY, MODE_PARTIAL);
    }

    @Test
    void testOpenWithInvalidModeThrowsIllegalArgumentException() {
        when(inputParameters.getString(MODE, MODE_FULL_REPLACE)).thenReturn("BOGUS");

        assertThatThrownBy(() -> writer.open(inputParameters, connectionParameters, context, executionContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown sync mode 'BOGUS'");
    }

    private static KnowledgeBaseSource newSource(Long kbId) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setKnowledgeBaseId(kbId);

        return source;
    }

    private static KnowledgeBaseDocument newDocument(Long id, String payloadHash, Instant deletedAt) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setSourceId(SOURCE_ID_VALUE);
        document.setSourceRecordId("rec1");
        document.setSyncedPayloadHash(payloadHash);
        document.setDeletedAt(deletedAt);

        return document;
    }
}
