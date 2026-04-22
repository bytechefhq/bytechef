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

package com.bytechef.automation.workspacefile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workspacefile.config.AutomationWorkspaceFileQuotaProperties;
import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.domain.WorkspaceFileSource;
import com.bytechef.automation.workspacefile.domain.WorkspaceWorkspaceFile;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.file.storage.WorkspaceFileFileStorage;
import com.bytechef.automation.workspacefile.metric.WorkspaceFileMetrics;
import com.bytechef.automation.workspacefile.repository.WorkspaceWorkspaceFileRepository;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceFileFacadeTest {

    @Mock
    private WorkspaceFileService service;

    @Mock
    private WorkspaceWorkspaceFileRepository workspaceRepository;

    @Mock
    private WorkspaceFileFileStorage fileStorage;

    @Mock
    private WorkspaceFileMetrics metrics;

    private WorkspaceFileFacade facade;

    private AutomationWorkspaceFileQuotaProperties quota;

    @BeforeEach
    void setUp() {
        quota = new AutomationWorkspaceFileQuotaProperties(26_214_400L, 1_073_741_824L, 1_048_576L);

        facade = new WorkspaceFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());
    }

    @Test
    void testCreateFromUploadHappyPath() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("hello.txt", "workspace-files/hello.txt");

        when(fileStorage.storeFile(eq("hello.txt"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndName(eq(1L), anyString())).thenReturn(Optional.empty());
        when(service.create(any(WorkspaceFile.class), eq(1L))).thenAnswer(invocation -> {
            WorkspaceFile workspaceFile = invocation.getArgument(0);

            workspaceFile.setId(10L);

            return workspaceFile;
        });

        WorkspaceFile result = facade.createFromUpload(1L, "hello.txt", "text/plain", new ByteArrayInputStream(bytes));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("hello.txt");
        assertThat(result.getSizeBytes()).isEqualTo(bytes.length);
        assertThat(result.getSource()).isEqualTo(WorkspaceFileSource.USER_UPLOAD);
        assertThat(result.getMimeType()).isNotNull();
        assertThat(result.getFile()).isEqualTo(stored);

        verify(metrics).recordCreate(eq(WorkspaceFileSource.USER_UPLOAD), anyString());
    }

    @Test
    void testCreateFromUploadRejectsWhenSingleFileOverLimit() {
        quota = new AutomationWorkspaceFileQuotaProperties(1024L, 1_073_741_824L, 1_048_576L);

        facade = new WorkspaceFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        byte[] bytes = new byte[2048];

        when(service.fetchByWorkspaceIdAndName(eq(1L), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> facade.createFromUpload(1L, "big.bin", "application/octet-stream", new ByteArrayInputStream(bytes)))
                .isInstanceOf(WorkspaceFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(service, never()).create(any(WorkspaceFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadRejectsWhenWorkspaceTotalOver() {
        quota = new AutomationWorkspaceFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L);

        facade = new WorkspaceFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        byte[] bytes = new byte[2];

        when(service.fetchByWorkspaceIdAndName(eq(1L), anyString())).thenReturn(Optional.empty());
        when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(9999L);

        assertThatThrownBy(
            () -> facade.createFromUpload(1L, "small.txt", "text/plain", new ByteArrayInputStream(bytes)))
                .isInstanceOf(WorkspaceFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(service, never()).create(any(WorkspaceFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadDeletesBlobIfDbWriteFails() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("a.txt", "workspace-files/a.txt");

        when(fileStorage.storeFile(eq("a.txt"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndName(eq(1L), anyString())).thenReturn(Optional.empty());
        when(service.create(any(WorkspaceFile.class), eq(1L)))
            .thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> facade.createFromUpload(1L, "a.txt", "text/plain", new ByteArrayInputStream(bytes)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db failure");

        verify(fileStorage).deleteFile(stored);
        verify(metrics, never()).recordCreate(any(WorkspaceFileSource.class), anyString());
    }

    @Test
    void testCreateFromAiSetsProvenanceAndUsesProvidedMime() {
        FileEntry stored = new FileEntry("note.md", "workspace-files/note.md");

        when(fileStorage.storeFile(eq("note.md"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndName(eq(1L), anyString())).thenReturn(Optional.empty());
        when(service.create(any(WorkspaceFile.class), eq(1L))).thenAnswer(invocation -> {
            WorkspaceFile workspaceFile = invocation.getArgument(0);

            workspaceFile.setId(99L);

            return workspaceFile;
        });

        WorkspaceFile result = facade.createFromAi(
            1L, "note.md", "text/markdown", "# Hello", (short) 3, "Write me a greeting");

        assertThat(result.getSource()).isEqualTo(WorkspaceFileSource.AI_GENERATED);
        assertThat(result.getMimeType()).isEqualTo("text/markdown");
        assertThat(result.getGeneratedByAgentSource()).isEqualTo((short) 3);
        assertThat(result.getGeneratedFromPrompt()).isEqualTo("Write me a greeting");

        verify(metrics).recordCreate(eq(WorkspaceFileSource.AI_GENERATED), eq("text/markdown"));
    }

    @Test
    void testUpdateContentEnforcesDeltaQuota() {
        quota = new AutomationWorkspaceFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L);

        facade = new WorkspaceFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        WorkspaceFile existing = new WorkspaceFile();

        existing.setId(5L);
        existing.setName("note.md");
        existing.setSizeBytes(900);
        existing.setFile(new FileEntry("note.md", "workspace-files/old.md"));

        WorkspaceWorkspaceFile link = new WorkspaceWorkspaceFile(5L, 1L);

        when(service.findById(5L)).thenReturn(existing);
        when(workspaceRepository.findByWorkspaceFileId(5L)).thenReturn(Optional.of(link));
        when(service.sumSizeBytesByWorkspaceId(1L)).thenReturn(9000L);

        byte[] newBytes = new byte[5900];

        assertThatThrownBy(
            () -> facade.updateContent(5L, "text/markdown", new ByteArrayInputStream(newBytes)))
                .isInstanceOf(WorkspaceFileQuotaExceededException.class);

        verify(fileStorage, never()).storeFile(anyString(), any(InputStream.class));
        verify(service, never()).update(any(WorkspaceFile.class));
    }

    @Test
    void testRenameCollisionAppendsSuffix() {
        WorkspaceFile existing = new WorkspaceFile();

        existing.setId(5L);
        existing.setName("old.md");

        WorkspaceFile other = new WorkspaceFile();

        other.setId(6L);
        other.setName("foo.md");

        WorkspaceWorkspaceFile link = new WorkspaceWorkspaceFile(5L, 1L);

        when(service.findById(5L)).thenReturn(existing);
        when(workspaceRepository.findByWorkspaceFileId(5L)).thenReturn(Optional.of(link));
        when(service.fetchByWorkspaceIdAndName(1L, "foo.md")).thenReturn(Optional.of(other));
        when(service.fetchByWorkspaceIdAndName(1L, "foo-2.md")).thenReturn(Optional.empty());
        when(service.update(any(WorkspaceFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceFile result = facade.rename(5L, "foo.md");

        assertThat(result.getName()).isEqualTo("foo-2.md");
    }

    @Test
    void testDeleteDeletesBlobBeforeRow() {
        FileEntry fileEntry = new FileEntry("x.txt", "workspace-files/x.txt");
        WorkspaceFile existing = new WorkspaceFile();

        existing.setId(11L);
        existing.setFile(fileEntry);

        when(service.findById(11L)).thenReturn(existing);

        facade.delete(11L);

        InOrder inOrder = inOrder(fileStorage, service);

        inOrder.verify(fileStorage)
            .deleteFile(fileEntry);
        inOrder.verify(service)
            .delete(11L);
    }

    @Test
    void testFindByIdDelegates() {
        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setId(42L);

        when(service.findById(42L)).thenReturn(workspaceFile);

        assertThat(facade.findById(42L)).isSameAs(workspaceFile);
    }

    @Test
    void testFindAllByWorkspaceIdDelegates() {
        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setId(1L);

        when(service.findAllByWorkspaceId(7L, null)).thenReturn(List.of(workspaceFile));
        when(service.findAllByWorkspaceId(eq(8L), anyList())).thenReturn(List.of(workspaceFile));

        assertThat(facade.findAllByWorkspaceId(7L, null)).hasSize(1);
        assertThat(facade.findAllByWorkspaceId(8L, List.of(2L, 3L))).hasSize(1);

        verify(service, times(1)).findAllByWorkspaceId(7L, null);
        verify(service, times(1)).findAllByWorkspaceId(8L, List.of(2L, 3L));
    }
}
