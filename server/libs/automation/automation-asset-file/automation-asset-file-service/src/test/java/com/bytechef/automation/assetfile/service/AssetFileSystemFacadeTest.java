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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.cleanup.AssetFileOrphanBlobRecorder;
import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.domain.AssetFileVersion;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.repository.AssetFileVersionRepository;
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
class AssetFileSystemFacadeTest {

    private static final Long ASSET_FILE_ID = 5L;
    private static final Long WORKSPACE_ID = 1L;
    private static final Long OTHER_WORKSPACE_ID = 2L;

    @Mock
    private AssetFileService assetFileService;

    @Mock
    private AssetFileFileStorage fileStorage;

    @Mock
    private AssetFileMetrics metrics;

    @Mock
    private AssetFileOrphanBlobRecorder orphanBlobRecorder;

    @Mock
    private AssetFileVersionRepository versionRepository;

    private AssetFileSystemFacade assetFileSystemFacade;

    private AutomationAssetFileQuotaProperties quota;

    @BeforeEach
    void setUp() {
        quota = new AutomationAssetFileQuotaProperties(26_214_400L, 1_073_741_824L, 1_048_576L, 10);

        assetFileSystemFacade = newAssetFileSystemFacade(new AutomationAssetFileSharingProperties(true));
    }

    private AssetFileSystemFacade newAssetFileSystemFacade(AutomationAssetFileSharingProperties sharingProperties) {
        AssetFileWriteSupport writeSupport = new AssetFileWriteSupport(
            assetFileService, fileStorage, metrics, orphanBlobRecorder, quota);

        return new AssetFileSystemFacadeImpl(
            assetFileService, fileStorage, metrics, versionRepository, writeSupport, quota, sharingProperties,
            new Tika());
    }

    @Test
    void testRenameInWorkspaceRefusesAFileOwnedByAnotherWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("a server-derived workspace that does not own the file must not be able to rename it")
            .isThrownBy(() -> assetFileSystemFacade.renameInWorkspace(ASSET_FILE_ID, WORKSPACE_ID, "new-name"));

        verify(assetFileService, never()).update(any());
    }

    @Test
    void testRenameInWorkspaceRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("degrading a missing workspace to unscoped is how listWorkflowExecutions returned an empty page "
                + "instead of refusing")
            .isThrownBy(() -> assetFileSystemFacade.renameInWorkspace(ASSET_FILE_ID, null, "new-name"));
    }

    @Test
    void testRenameInWorkspaceRenamesAFileOwnedByTheWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setName("old.md");

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(WORKSPACE_ID, 0, "new-name.md"))
            .thenReturn(Optional.empty());
        when(assetFileService.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetFile result = assetFileSystemFacade.renameInWorkspace(ASSET_FILE_ID, WORKSPACE_ID, "new-name.md");

        assertThat(result.getName()).isEqualTo("new-name.md");
    }

    @Test
    void testRenameInWorkspaceCollisionAppendsSuffix() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setName("old.md");
        assetFile.setWorkspaceId(WORKSPACE_ID);

        AssetFile other = new AssetFile();

        other.setId(6L);
        other.setName("foo.md");
        other.setWorkspaceId(WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(WORKSPACE_ID, 0, "foo.md"))
            .thenReturn(Optional.of(other));
        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(WORKSPACE_ID, 0, "foo-2.md"))
            .thenReturn(Optional.empty());
        when(assetFileService.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetFile result = assetFileSystemFacade.renameInWorkspace(ASSET_FILE_ID, WORKSPACE_ID, "foo.md");

        assertThat(result.getName()).isEqualTo("foo-2.md");
    }

    @Test
    void testDeleteInWorkspaceRefusesAFileOwnedByAnotherWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("a server-derived workspace that does not own the file must not be able to delete it")
            .isThrownBy(() -> assetFileSystemFacade.deleteInWorkspace(ASSET_FILE_ID, WORKSPACE_ID));

        verify(assetFileService, never()).delete(any());
    }

    @Test
    void testDeleteInWorkspaceRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("degrading a missing workspace to unscoped is how listWorkflowExecutions returned an empty page "
                + "instead of refusing")
            .isThrownBy(() -> assetFileSystemFacade.deleteInWorkspace(ASSET_FILE_ID, null));
    }

    @Test
    void testDeleteInWorkspaceDeletesAFileOwnedByTheWorkspace() {
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setFile(fileEntry);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID)).thenReturn(List.of());

        assetFileSystemFacade.deleteInWorkspace(ASSET_FILE_ID, WORKSPACE_ID);

        InOrder order = inOrder(fileStorage, assetFileService);

        order.verify(assetFileService)
            .delete(ASSET_FILE_ID);
        order.verify(fileStorage)
            .deleteFile(fileEntry);
    }

    @Test
    void testDeleteInWorkspaceSchedulesVersionBlobDeletes() {
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        FileEntry versionFileEntry = new FileEntry("x.txt", "asset-files/x-v1.txt");

        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setFile(fileEntry);

        AssetFileVersion version = new AssetFileVersion();

        version.setId(1L);
        version.setAssetFileId(ASSET_FILE_ID);
        version.setFile(versionFileEntry);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID))
            .thenReturn(List.of(version));

        assetFileSystemFacade.deleteInWorkspace(ASSET_FILE_ID, WORKSPACE_ID);

        verify(fileStorage).deleteFile(fileEntry);
        verify(fileStorage).deleteFile(versionFileEntry);
    }

    @Test
    void testDeleteInWorkspaceEnqueuesOrphanWhenBlobDeleteFails() {
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setFile(fileEntry);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("storage down"))
            .when(fileStorage)
            .deleteFile(fileEntry);

        assetFileSystemFacade.deleteInWorkspace(ASSET_FILE_ID, WORKSPACE_ID);

        verify(orphanBlobRecorder).record(fileEntry);
        verify(metrics).recordBlobOrphan("RuntimeException");
    }

    @Test
    void testDownloadContentInWorkspaceRefusesAFileOwnedByAnotherWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("a server-derived workspace that does not own the file must not be able to download it")
            .isThrownBy(() -> assetFileSystemFacade.downloadContentInWorkspace(ASSET_FILE_ID, WORKSPACE_ID));

        verify(fileStorage, never()).getInputStream(any());
    }

    @Test
    void testDownloadContentInWorkspaceRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("degrading a missing workspace to unscoped is how listWorkflowExecutions returned an empty page "
                + "instead of refusing")
            .isThrownBy(() -> assetFileSystemFacade.downloadContentInWorkspace(ASSET_FILE_ID, null));
    }

    @Test
    void testDownloadContentInWorkspaceStreamsAFileOwnedByTheWorkspace() {
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setFile(fileEntry);

        InputStream inputStream = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8));

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(fileStorage.getInputStream(fileEntry)).thenReturn(inputStream);

        assertThat(assetFileSystemFacade.downloadContentInWorkspace(ASSET_FILE_ID, WORKSPACE_ID))
            .isSameAs(inputStream);
    }

    @Test
    void testUpdateContentInWorkspaceRefusesAFileOwnedByAnotherWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        InputStream data = new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("a server-derived workspace that does not own the file must not be able to update its content")
            .isThrownBy(() -> assetFileSystemFacade.updateContentInWorkspace(
                ASSET_FILE_ID, WORKSPACE_ID, "text/plain", data));

        verify(assetFileService, never()).update(any());
    }

    @Test
    void testUpdateContentInWorkspaceRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        InputStream data = new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8));

        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("degrading a missing workspace to unscoped is how listWorkflowExecutions returned an empty page "
                + "instead of refusing")
            .isThrownBy(() -> assetFileSystemFacade.updateContentInWorkspace(
                ASSET_FILE_ID, null, "text/plain", data));
    }

    @Test
    void testUpdateContentInWorkspaceReplacesContentOfAFileOwnedByTheWorkspace() {
        FileEntry previousFile = new FileEntry("note.md", "asset-files/old.md");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);
        assetFile.setName("note.md");
        assetFile.setMimeType("text/markdown");
        assetFile.setSizeBytes(100);
        assetFile.setFile(previousFile);

        FileEntry stored = new FileEntry("note.md", "asset-files/new.md");

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(fileStorage.storeFile(eq("note.md"), any(InputStream.class))).thenReturn(stored);
        when(assetFileService.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findFirstByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID))
            .thenReturn(Optional.empty());
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID)).thenReturn(List.of());

        InputStream data = new ByteArrayInputStream("new content".getBytes(StandardCharsets.UTF_8));

        AssetFile result = assetFileSystemFacade.updateContentInWorkspace(
            ASSET_FILE_ID, WORKSPACE_ID, "text/plain", data);

        assertThat(result.getFile()).isEqualTo(stored);

        ArgumentCaptor<AssetFileVersion> versionCaptor = ArgumentCaptor.forClass(AssetFileVersion.class);

        verify(versionRepository).save(versionCaptor.capture());

        AssetFileVersion snapshot = versionCaptor.getValue();

        assertThat(snapshot.getAssetFileId()).isEqualTo(ASSET_FILE_ID);
        assertThat(snapshot.getVersionNumber()).isEqualTo(1);
        assertThat(snapshot.getFile()).isEqualTo(previousFile);
        assertThat(snapshot.getMimeType()).isEqualTo("text/markdown");
        assertThat(snapshot.getSizeBytes()).isEqualTo(100);

        verify(fileStorage, never()).deleteFile(previousFile);
    }

    @Test
    void testUpdateContentInWorkspaceEnforcesDeltaQuota() {
        quota = new AutomationAssetFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L, 10);

        assetFileSystemFacade = newAssetFileSystemFacade(new AutomationAssetFileSharingProperties(true));

        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setName("note.md");
        assetFile.setSizeBytes(900);
        assetFile.setFile(new FileEntry("note.md", "asset-files/old.md"));
        assetFile.setWorkspaceId(WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(assetFileService.sumSizeBytesByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0)).thenReturn(9000L);

        byte[] newBytes = new byte[5900];

        assertThatThrownBy(
            () -> assetFileSystemFacade.updateContentInWorkspace(
                ASSET_FILE_ID, WORKSPACE_ID, "text/markdown", new ByteArrayInputStream(newBytes)))
                    .isInstanceOf(AssetFileQuotaExceededException.class);

        verify(fileStorage, never()).storeFile(anyString(), any(InputStream.class));
        verify(assetFileService, never()).update(any(AssetFile.class));
    }

    @Test
    void testUpdateContentInWorkspacePrunesVersionsBeyondCap() {
        quota = new AutomationAssetFileQuotaProperties(26_214_400L, 1_073_741_824L, 1_048_576L, 1);

        assetFileSystemFacade = newAssetFileSystemFacade(new AutomationAssetFileSharingProperties(true));

        FileEntry previousFile = new FileEntry("note.md", "asset-files/old.md");
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setName("note.md");
        assetFile.setSizeBytes(100);
        assetFile.setFile(previousFile);
        assetFile.setWorkspaceId(WORKSPACE_ID);

        AssetFileVersion newest = new AssetFileVersion();

        newest.setId(200L);
        newest.setAssetFileId(ASSET_FILE_ID);
        newest.setVersionNumber(2);
        newest.setFile(previousFile);

        AssetFileVersion oldest = new AssetFileVersion();

        oldest.setId(100L);
        oldest.setAssetFileId(ASSET_FILE_ID);
        oldest.setVersionNumber(1);
        oldest.setFile(new FileEntry("note.md", "asset-files/ancient.md"));

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(fileStorage.storeFile(eq("note.md"), any(InputStream.class)))
            .thenReturn(new FileEntry("note.md", "asset-files/new.md"));
        when(assetFileService.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findFirstByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID))
            .thenReturn(Optional.of(oldest));
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(ASSET_FILE_ID))
            .thenReturn(List.of(newest, oldest));

        assetFileSystemFacade.updateContentInWorkspace(
            ASSET_FILE_ID, WORKSPACE_ID, "text/markdown",
            new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

        verify(versionRepository).deleteById(100L);
        verify(fileStorage).deleteFile(oldest.getFile());
    }

    @Test
    void testCreateFromUploadPersistsFileUnderTheWorkspace() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("hello.txt", "asset-files/hello.txt");

        when(fileStorage.storeFile(eq("hello.txt"), any(InputStream.class))).thenReturn(stored);
        when(assetFileService.sumSizeBytesByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0)).thenReturn(0L);
        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(eq(WORKSPACE_ID), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(assetFileService.create(any(AssetFile.class), eq(WORKSPACE_ID))).thenAnswer(invocation -> {
            AssetFile assetFile = invocation.getArgument(0);

            assetFile.setId(ASSET_FILE_ID);
            assetFile.setWorkspaceId(invocation.getArgument(1));

            return assetFile;
        });

        AssetFile result = assetFileSystemFacade.createFromUpload(
            WORKSPACE_ID, 0, "hello.txt", "text/plain", new ByteArrayInputStream(bytes));

        assertThat(result.getId()).isEqualTo(ASSET_FILE_ID);
        assertThat(result.getName()).isEqualTo("hello.txt");
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getMimeType()).isNotNull();
        assertThat(result.getFile()).isEqualTo(stored);

        verify(metrics).recordCreate(eq(AssetFileSource.USER_UPLOAD), anyString());
    }

    @Test
    void testCreateFromUploadRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("rejecting up front, not after the payload has been buffered and the blob written")
            .isThrownBy(
                () -> assetFileSystemFacade.createFromUpload(
                    null, 0, "note.txt", "text/plain",
                    new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))));

        verifyNoInteractions(fileStorage);
        verify(assetFileService, never()).create(any(AssetFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadRejectsWhenSingleFileOverLimit() {
        quota = new AutomationAssetFileQuotaProperties(1024L, 1_073_741_824L, 1_048_576L, 10);
        assetFileSystemFacade = newAssetFileSystemFacade(new AutomationAssetFileSharingProperties(true));

        byte[] bytes = new byte[2048];

        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(eq(WORKSPACE_ID), anyInt(), anyString()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> assetFileSystemFacade.createFromUpload(
                WORKSPACE_ID, 0, "big.bin", "application/octet-stream", new ByteArrayInputStream(bytes)))
                    .isInstanceOf(AssetFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(assetFileService, never()).create(any(AssetFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadRejectsWhenWorkspaceTotalOver() {
        quota = new AutomationAssetFileQuotaProperties(1_000_000L, 10_000L, 1_048_576L, 10);
        assetFileSystemFacade = newAssetFileSystemFacade(new AutomationAssetFileSharingProperties(true));

        byte[] bytes = new byte[2];

        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(eq(WORKSPACE_ID), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(assetFileService.sumSizeBytesByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0)).thenReturn(9999L);

        assertThatThrownBy(
            () -> assetFileSystemFacade.createFromUpload(
                WORKSPACE_ID, 0, "small.txt", "text/plain", new ByteArrayInputStream(bytes)))
                    .isInstanceOf(AssetFileQuotaExceededException.class);

        verifyNoInteractions(fileStorage);
        verify(assetFileService, never()).create(any(AssetFile.class), anyLong());
    }

    @Test
    void testCreateFromUploadDeletesBlobIfDbWriteFails() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("a.txt", "asset-files/a.txt");

        when(fileStorage.storeFile(eq("a.txt"), any(InputStream.class))).thenReturn(stored);
        when(assetFileService.sumSizeBytesByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0)).thenReturn(0L);
        when(assetFileService.fetchByWorkspaceIdAndEnvironmentAndName(eq(WORKSPACE_ID), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(assetFileService.create(any(AssetFile.class), eq(WORKSPACE_ID)))
            .thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(
            () -> assetFileSystemFacade.createFromUpload(
                WORKSPACE_ID, 0, "a.txt", "text/plain", new ByteArrayInputStream(bytes)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db failure");

        verify(fileStorage).deleteFile(stored);
        verify(metrics, never()).recordCreate(any(AssetFileSource.class), anyString());
    }

    @Test
    void testFindAllByWorkspaceIdAndEnvironmentDelegates() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);

        when(assetFileService.findAllByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0, null))
            .thenReturn(List.of(assetFile));

        assertThat(assetFileSystemFacade.findAllByWorkspaceIdAndEnvironment(WORKSPACE_ID, 0, null)).hasSize(1);
    }

    @Test
    void testFindAllByWorkspaceIdAndEnvironmentRejectsANullWorkspaceRatherThanTreatingItAsUnscoped() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .as("degrading a missing workspace to unscoped is how listWorkflowExecutions returned an empty page "
                + "instead of refusing")
            .isThrownBy(() -> assetFileSystemFacade.findAllByWorkspaceIdAndEnvironment(null, 0, null));

        verifyNoInteractions(assetFileService);
    }

    @Test
    void testFindByIdInWorkspaceReturnsAFileOwnedByTheWorkspace() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(WORKSPACE_ID);

        when(assetFileService.findById(ASSET_FILE_ID)).thenReturn(assetFile);

        assertThat(assetFileSystemFacade.findByIdInWorkspace(ASSET_FILE_ID, WORKSPACE_ID)).isSameAs(assetFile);
    }

    @Test
    void testFetchByPublicLinkTokenReturnsEmptyWhenKillSwitchOff() {
        AssetFileSystemFacade facadeWithSharingDisabled = newAssetFileSystemFacade(
            new AutomationAssetFileSharingProperties(false));

        assertThat(facadeWithSharingDisabled.fetchByPublicLinkToken("some-token")).isEmpty();

        verifyNoInteractions(assetFileService);
    }

    @Test
    void testFetchByPublicLinkTokenDelegatesWhenEnabled() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);

        when(assetFileService.fetchByPublicLinkToken("some-token")).thenReturn(Optional.of(assetFile));

        assertThat(assetFileSystemFacade.fetchByPublicLinkToken("some-token")).contains(assetFile);
    }
}
