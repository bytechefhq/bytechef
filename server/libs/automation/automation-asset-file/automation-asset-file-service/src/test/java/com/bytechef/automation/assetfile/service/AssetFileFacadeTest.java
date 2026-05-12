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

package com.bytechef.automation.assetfile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.domain.WorkspaceAssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.repository.WorkspaceAssetFileRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AssetFileFacadeTest {

    @Mock
    private AssetFileService service;

    @Mock
    private WorkspaceAssetFileRepository workspaceRepository;

    @Mock
    private AssetFileFileStorage fileStorage;

    @Mock
    private AssetFileMetrics metrics;

    private AssetFileFacade facade;

    private AutomationAssetFileQuotaProperties quota;

    @BeforeEach
    void setUp() {
        quota = new AutomationAssetFileQuotaProperties(26_214_400L, 1_073_741_824L, 1_048_576L);

        facade = new AssetFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());
    }

    @Test
    void testCreateFromUploadHappyPath() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("hello.txt", "asset-files/hello.txt");

        when(fileStorage.storeFile(eq("hello.txt"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(service.create(any(AssetFile.class), eq(1L))).thenAnswer(invocation -> {
            AssetFile assetFile = invocation.getArgument(0);

            assetFile.setId(10L);

            return assetFile;
        });

        AssetFile result = facade.createFromUpload(1L, 0, "hello.txt", "text/plain", new ByteArrayInputStream(bytes));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("hello.txt");
        assertThat(result.getSizeBytes()).isEqualTo(bytes.length);
        assertThat(result.getSource()).isEqualTo(AssetFileSource.USER_UPLOAD);
        assertThat(result.getMimeType()).isNotNull();
        assertThat(result.getFile()).isEqualTo(stored);

        verify(metrics).recordCreate(eq(AssetFileSource.USER_UPLOAD), anyString());
    }

    @Test
    void testCreateFromUploadRejectsWhenSingleFileOverLimit() {
        quota = new AutomationAssetFileQuotaProperties(1024L, 1_073_741_824L, 1_048_576L);

        facade = new AssetFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        byte[] bytes = new byte[2048];

        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> facade.createFromUpload(1L, 0, "big.bin", "application/octet-stream",
                new ByteArrayInputStream(bytes)))
                    .isInstanceOf(AssetFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(service, never()).create(any(AssetFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadRejectsWhenWorkspaceTotalOver() {
        quota = new AutomationAssetFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L);

        facade = new AssetFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        byte[] bytes = new byte[2];

        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(9999L);

        assertThatThrownBy(
            () -> facade.createFromUpload(1L, 0, "small.txt", "text/plain", new ByteArrayInputStream(bytes)))
                .isInstanceOf(AssetFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(service, never()).create(any(AssetFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadDeletesBlobIfDbWriteFails() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("a.txt", "asset-files/a.txt");

        when(fileStorage.storeFile(eq("a.txt"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(service.create(any(AssetFile.class), eq(1L)))
            .thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> facade.createFromUpload(1L, 0, "a.txt", "text/plain", new ByteArrayInputStream(bytes)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db failure");

        verify(fileStorage).deleteFile(stored);
        verify(metrics, never()).recordCreate(any(AssetFileSource.class), anyString());
    }

    @Test
    void testCreateFromAiSetsProvenanceAndUsesProvidedMime() {
        FileEntry stored = new FileEntry("note.md", "asset-files/note.md");

        when(fileStorage.storeFile(eq("note.md"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(service.create(any(AssetFile.class), eq(1L))).thenAnswer(invocation -> {
            AssetFile assetFile = invocation.getArgument(0);

            assetFile.setId(99L);

            return assetFile;
        });

        AssetFile result = facade.createFromAi(
            1L, 0, "note.md", "text/markdown", "# Hello", null, null, (short) 3, "Write me a greeting");

        assertThat(result.getSource()).isEqualTo(AssetFileSource.AI_GENERATED);
        assertThat(result.getMimeType()).isEqualTo("text/markdown");
        assertThat(result.getGeneratedByAgentSource()).isEqualTo((short) 3);
        assertThat(result.getGeneratedFromPrompt()).isEqualTo("Write me a greeting");

        verify(metrics).recordCreate(eq(AssetFileSource.AI_GENERATED), eq("text/markdown"));
    }

    @Test
    void testCreateBinaryFromAiStoresBytesDirectly() throws Exception {
        byte[] data = new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
        };
        FileEntry stored = new FileEntry("icon.png", "asset-files/icon.png");

        when(fileStorage.storeFile(eq("icon.png"), any(InputStream.class))).thenReturn(stored);
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(0L);
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(eq(1L), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(service.create(any(AssetFile.class), eq(1L))).thenAnswer(invocation -> {
            AssetFile assetFile = invocation.getArgument(0);

            assetFile.setId(77L);

            return assetFile;
        });

        AssetFile result = facade.createBinaryFromAi(
            1L, 0, "icon.png", "image/png", data, null, null, (short) 2, "create an icon");

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getName()).isEqualTo("icon.png");
        assertThat(result.getMimeType()).isEqualTo("image/png");
        assertThat(result.getSizeBytes()).isEqualTo(data.length);
        assertThat(result.getSource()).isEqualTo(AssetFileSource.AI_GENERATED);
        assertThat(result.getGeneratedByAgentSource()).isEqualTo((short) 2);
        assertThat(result.getGeneratedFromPrompt()).isEqualTo("create an icon");

        ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);

        verify(fileStorage).storeFile(eq("icon.png"), streamCaptor.capture());

        assertThat(streamCaptor.getValue()
            .readAllBytes()).isEqualTo(data);

        verify(metrics).recordCreate(eq(AssetFileSource.AI_GENERATED), eq("image/png"));
    }

    @Test
    void testUpdateContentEnforcesDeltaQuota() {
        quota = new AutomationAssetFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L);

        facade = new AssetFileFacadeImpl(service, workspaceRepository, fileStorage, metrics, quota, new Tika());

        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("note.md");
        existing.setSizeBytes(900);
        existing.setFile(new FileEntry("note.md", "asset-files/old.md"));

        WorkspaceAssetFile link = new WorkspaceAssetFile(5L, 1L);

        when(service.findById(5L)).thenReturn(existing);
        when(workspaceRepository.findByAssetFileId(5L)).thenReturn(Optional.of(link));
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(9000L);

        byte[] newBytes = new byte[5900];

        assertThatThrownBy(
            () -> facade.updateContent(5L, "text/markdown", new ByteArrayInputStream(newBytes)))
                .isInstanceOf(AssetFileQuotaExceededException.class);

        verify(fileStorage, never()).storeFile(anyString(), any(InputStream.class));
        verify(service, never()).update(any(AssetFile.class));
    }

    @Test
    void testRenameCollisionAppendsSuffix() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("old.md");

        AssetFile other = new AssetFile();

        other.setId(6L);
        other.setName("foo.md");

        WorkspaceAssetFile link = new WorkspaceAssetFile(5L, 1L);

        when(service.findById(5L)).thenReturn(existing);
        when(workspaceRepository.findByAssetFileId(5L)).thenReturn(Optional.of(link));
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(1L, 0, "foo.md")).thenReturn(Optional.of(other));
        when(service.fetchByWorkspaceIdAndEnvironmentAndName(1L, 0, "foo-2.md")).thenReturn(Optional.empty());
        when(service.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetFile result = facade.rename(5L, "foo.md");

        assertThat(result.getName()).isEqualTo("foo-2.md");
    }

    @Test
    void testDeleteDeletesRowBeforeBlob() {
        // Pins the post-fix ordering: the DB row must be deleted FIRST so a transaction rollback can never
        // leave a row pointing at a missing blob. The blob delete is then deferred to afterCommit; outside
        // an active transaction (this unit test), the facade falls back to deleting the blob synchronously
        // after the DB delete returns.
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        AssetFile existing = new AssetFile();

        existing.setId(11L);
        existing.setFile(fileEntry);

        when(service.findById(11L)).thenReturn(existing);

        facade.delete(11L);

        InOrder inOrder = inOrder(fileStorage, service);

        inOrder.verify(service)
            .delete(11L);
        inOrder.verify(fileStorage)
            .deleteFile(fileEntry);
    }

    @Test
    void testFindByIdDelegates() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(42L);

        when(service.findById(42L)).thenReturn(assetFile);

        assertThat(facade.findById(42L)).isSameAs(assetFile);
    }

    @Test
    void testFindAllByWorkspaceIdDelegates() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(1L);

        when(service.findAllByWorkspaceIdAndEnvironment(7L, 0, null)).thenReturn(List.of(assetFile));
        when(service.findAllByWorkspaceIdAndEnvironment(eq(8L), anyInt(), anyList())).thenReturn(List.of(assetFile));

        assertThat(facade.findAllByWorkspaceIdAndEnvironment(7L, 0, null)).hasSize(1);
        assertThat(facade.findAllByWorkspaceIdAndEnvironment(8L, 0, List.of(2L, 3L))).hasSize(1);

        verify(service, times(1)).findAllByWorkspaceIdAndEnvironment(7L, 0, null);
        verify(service, times(1)).findAllByWorkspaceIdAndEnvironment(8L, 0, List.of(2L, 3L));
    }

    @Test
    void testGetOwningWorkspaceIdReturnsLink() {
        WorkspaceAssetFile link = new WorkspaceAssetFile(42L, 11L);

        when(workspaceRepository.findByAssetFileId(42L)).thenReturn(Optional.of(link));

        assertThat(facade.getOwningWorkspaceId(42L)).isEqualTo(11L);
    }

    @Test
    void testGetOwningWorkspaceIdThrowsWhenLinkMissing() {
        when(workspaceRepository.findByAssetFileId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getOwningWorkspaceId(42L))
            .isInstanceOf(com.bytechef.automation.assetfile.exception.AssetFileNotFoundException.class)
            .hasMessageContaining("42");
    }
}
