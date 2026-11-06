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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private AssetFileFileStorage fileStorage;

    @Mock
    private AssetFileMetrics metrics;

    @Mock
    private AssetFileOrphanBlobRecorder orphanBlobRecorder;

    @Mock
    private AssetFileVersionRepository versionRepository;

    @Mock
    private AssetFileSystemFacade assetFileSystemFacade;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceFacade workspaceFacade;

    private AssetFileFacade facade;

    private AutomationAssetFileQuotaProperties quota;

    private AutomationAssetFileSharingProperties sharingProperties =
        new AutomationAssetFileSharingProperties(true);

    @BeforeEach
    void setUp() {
        quota = new AutomationAssetFileQuotaProperties(26_214_400L, 1_073_741_824L, 1_048_576L, 10);

        User currentUser = new User();

        currentUser.setId(1L);

        lenient().when(userService.fetchCurrentUser())
            .thenReturn(Optional.of(currentUser));
        lenient().when(workspaceFacade.getUserWorkspaces(anyLong()))
            .thenReturn(List.of(
                new Workspace(1L, "workspace-1"), new Workspace(7L, "workspace-7"),
                new Workspace(8L, "workspace-8")));

        facade = newFacade();
    }

    private AssetFileFacade newFacade() {
        AssetFileWriteSupport writeSupport = new AssetFileWriteSupport(
            service, fileStorage, metrics, orphanBlobRecorder, quota);

        return new AssetFileFacadeImpl(
            service, fileStorage, metrics, versionRepository, assetFileSystemFacade, writeSupport,
            quota, sharingProperties, userService, workspaceFacade);
    }

    @Test
    void testCreateFromUploadHappyPath() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        FileEntry stored = new FileEntry("hello.txt", "asset-files/hello.txt");
        AssetFile expected = new AssetFile();

        expected.setId(10L);
        expected.setName("hello.txt");
        expected.setSizeBytes(bytes.length);
        expected.setSource(AssetFileSource.USER_UPLOAD);
        expected.setMimeType("text/plain");
        expected.setFile(stored);
        expected.setWorkspaceId(1L);

        when(assetFileSystemFacade.createFromUpload(
            eq(1L), eq(0), eq("hello.txt"), eq("text/plain"), any(InputStream.class)))
                .thenReturn(expected);

        AssetFile result = facade.createFromUpload(1L, 0, "hello.txt", "text/plain", new ByteArrayInputStream(bytes));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("hello.txt");
        assertThat(result.getSizeBytes()).isEqualTo(bytes.length);
        assertThat(result.getSource()).isEqualTo(AssetFileSource.USER_UPLOAD);
        assertThat(result.getMimeType()).isNotNull();
        assertThat(result.getFile()).isEqualTo(stored);
        assertThat(result.getWorkspaceId()).isEqualTo(1L);
    }

    @Test
    void testCreateFromUploadPropagatesExceptionFromSystemFacade() {
        byte[] bytes = "data".getBytes(StandardCharsets.UTF_8);

        when(assetFileSystemFacade.createFromUpload(
            eq(1L), eq(0), eq("a.txt"), eq("text/plain"), any(InputStream.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> facade.createFromUpload(1L, 0, "a.txt", "text/plain", new ByteArrayInputStream(bytes)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("db failure");

        verify(assetFileSystemFacade).createFromUpload(
            eq(1L), eq(0), eq("a.txt"), eq("text/plain"), any(InputStream.class));
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
            assetFile.setWorkspaceId(invocation.getArgument(1));

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
            assetFile.setWorkspaceId(invocation.getArgument(1));

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
    void testUpdateContentPropagatesExceptionFromSystemFacade() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("note.md");
        existing.setSizeBytes(900);
        existing.setFile(new FileEntry("note.md", "asset-files/old.md"));
        existing.setWorkspaceId(1L);

        when(service.findById(5L)).thenReturn(existing);
        when(assetFileSystemFacade.updateContentInWorkspace(
            eq(5L), eq(1L), eq("text/markdown"), any(InputStream.class)))
                .thenThrow(new AssetFileQuotaExceededException(
                    "Workspace total 14000 would exceed limit 10000", 14000, 10000));

        byte[] newBytes = new byte[5900];

        assertThatThrownBy(
            () -> facade.updateContent(5L, "text/markdown", new ByteArrayInputStream(newBytes)))
                .isInstanceOf(AssetFileQuotaExceededException.class);

        verify(assetFileSystemFacade).updateContentInWorkspace(
            eq(5L), eq(1L), eq("text/markdown"), any(InputStream.class));
    }

    @Test
    void testRenameDelegatesToSystemFacadeAndReturnsItsResult() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("old.md");
        existing.setWorkspaceId(1L);

        AssetFile renamed = new AssetFile();

        renamed.setId(5L);
        renamed.setName("foo-2.md");
        renamed.setWorkspaceId(1L);

        when(service.findById(5L)).thenReturn(existing);
        when(assetFileSystemFacade.renameInWorkspace(5L, 1L, "foo.md")).thenReturn(renamed);

        AssetFile result = facade.rename(5L, "foo.md");

        assertThat(result.getName()).isEqualTo("foo-2.md");

        verify(assetFileSystemFacade).renameInWorkspace(5L, 1L, "foo.md");
    }

    @Test
    void testDeleteDelegatesToSystemFacadeWithResolvedWorkspace() {
        FileEntry fileEntry = new FileEntry("x.txt", "asset-files/x.txt");
        AssetFile existing = new AssetFile();

        existing.setId(11L);
        existing.setFile(fileEntry);
        existing.setWorkspaceId(1L);

        when(service.findById(11L)).thenReturn(existing);

        facade.delete(11L);

        verify(assetFileSystemFacade).deleteInWorkspace(11L, 1L);
    }

    @Test
    void testFindByIdDelegates() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(42L);
        assetFile.setWorkspaceId(1L);

        when(service.findById(42L)).thenReturn(assetFile);

        assertThat(facade.findById(42L)).isSameAs(assetFile);
    }

    @Test
    void testFindAllByWorkspaceIdDelegates() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(1L);

        when(assetFileSystemFacade.findAllByWorkspaceIdAndEnvironment(7L, 0, null)).thenReturn(List.of(assetFile));
        when(assetFileSystemFacade.findAllByWorkspaceIdAndEnvironment(eq(8L), anyInt(), anyList()))
            .thenReturn(List.of(assetFile));

        assertThat(facade.findAllByWorkspaceIdAndEnvironment(7L, 0, null)).hasSize(1);
        assertThat(facade.findAllByWorkspaceIdAndEnvironment(8L, 0, List.of(2L, 3L))).hasSize(1);

        verify(assetFileSystemFacade, times(1)).findAllByWorkspaceIdAndEnvironment(7L, 0, null);
        verify(assetFileSystemFacade, times(1)).findAllByWorkspaceIdAndEnvironment(8L, 0, List.of(2L, 3L));
    }

    @Test
    void testBareIdOperationResolvesOwnerFromTheRowsWorkspaceLink() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(42L);
        assetFile.setWorkspaceId(11L);

        when(service.findById(42L)).thenReturn(assetFile);

        assertThatThrownBy(() -> facade.findById(42L))
            .as("workspace 11 is not among the caller's workspaces, so the owner read off the row denies the call")
            .isInstanceOf(AssetFileNotFoundException.class)
            .hasMessageContaining("42");

        assetFile.setWorkspaceId(1L);

        assertThat(facade.findById(42L)).isSameAs(assetFile);
    }

    @Test
    void testBareIdOperationThrowsNotFoundWhenTheIdIsUnknown() {
        when(service.findById(42L)).thenThrow(new IllegalArgumentException("AssetFile 42 not found"));

        assertThatThrownBy(() -> facade.findById(42L))
            .isInstanceOf(AssetFileNotFoundException.class)
            .hasMessageContaining("42");
    }

    @Test
    void testBareIdOperationThrowsNotFoundWhenTheRowHasNoWorkspaceLink() {
        AssetFile unlinked = new AssetFile();

        unlinked.setId(42L);

        when(service.findById(42L)).thenReturn(unlinked);

        assertThatThrownBy(() -> facade.findById(42L))
            .as("a row with no owning workspace has no membership that could be satisfied, so it is not found")
            .isInstanceOf(AssetFileNotFoundException.class)
            .hasMessageContaining("42");
    }

    @Test
    void testTheThreeBareIdRefusalsAreIndistinguishable() {
        AssetFile unlinked = new AssetFile();

        unlinked.setId(42L);

        AssetFile foreign = new AssetFile();

        foreign.setId(42L);
        foreign.setWorkspaceId(11L);

        doThrow(new IllegalArgumentException("AssetFile 42 not found")).when(service)
            .findById(42L);

        Throwable unknownId = catchThrowable(() -> facade.findById(42L));

        doReturn(unlinked).when(service)
            .findById(42L);

        Throwable nullOwningWorkspace = catchThrowable(() -> facade.findById(42L));

        doReturn(foreign).when(service)
            .findById(42L);

        Throwable nonMember = catchThrowable(() -> facade.findById(42L));

        assertThat(List.of(unknownId, nullOwningWorkspace, nonMember))
            .as("all three refusals must be AssetFileNotFoundException - a different type is itself the oracle")
            .allSatisfy(throwable -> assertThat(throwable).isInstanceOf(AssetFileNotFoundException.class));

        assertThat(nullOwningWorkspace.getMessage())
            .as("a row with no owning workspace must be indistinguishable from an id that does not exist")
            .isEqualTo(unknownId.getMessage());

        assertThat(nonMember.getMessage())
            .as("a file owned by another workspace must be indistinguishable from an id that does not exist")
            .isEqualTo(unknownId.getMessage());
    }

    @Test
    void testUpdateContentDelegatesToSystemFacadeAndReturnsItsResult() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("note.md");
        existing.setSizeBytes(100);
        existing.setFile(new FileEntry("note.md", "asset-files/old.md"));
        existing.setWorkspaceId(1L);

        AssetFile updated = new AssetFile();

        updated.setId(5L);
        updated.setWorkspaceId(1L);

        when(service.findById(5L)).thenReturn(existing);
        when(assetFileSystemFacade.updateContentInWorkspace(
            eq(5L), eq(1L), eq("text/markdown"), any(InputStream.class)))
                .thenReturn(updated);

        AssetFile result = facade.updateContent(
            5L, "text/markdown", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

        assertThat(result).isSameAs(updated);

        verify(assetFileSystemFacade).updateContentInWorkspace(
            eq(5L), eq(1L), eq("text/markdown"), any(InputStream.class));
    }

    @Test
    void testRestoreVersionCopiesBytesAndSnapshotsCurrent() {
        FileEntry currentFile = new FileEntry("note.md", "asset-files/current.md");
        FileEntry versionFile = new FileEntry("note.md", "asset-files/v1.md");

        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setName("note.md");
        existing.setMimeType("text/markdown");
        existing.setSizeBytes(3);
        existing.setFile(currentFile);
        existing.setWorkspaceId(1L);

        AssetFileVersion version = new AssetFileVersion();

        version.setId(77L);
        version.setAssetFileId(5L);
        version.setVersionNumber(1);
        version.setFile(versionFile);
        version.setMimeType("text/markdown");
        version.setSizeBytes(9);

        when(service.findById(5L)).thenReturn(existing);
        when(versionRepository.findById(77L)).thenReturn(Optional.of(version));
        when(fileStorage.getInputStream(versionFile))
            .thenReturn(new ByteArrayInputStream("old bytes".getBytes(StandardCharsets.UTF_8)));
        when(service.sumSizeBytesByWorkspaceIdAndEnvironment(1L, 0)).thenReturn(3L);
        when(fileStorage.storeFile(eq("note.md"), any(InputStream.class)))
            .thenReturn(new FileEntry("note.md", "asset-files/restored.md"));
        when(service.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findFirstByAssetFileIdOrderByVersionNumberDesc(5L))
            .thenReturn(Optional.of(version));
        when(versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(5L)).thenReturn(List.of());

        AssetFile result = facade.restoreVersion(5L, 77L);

        assertThat(result.getSizeBytes()).isEqualTo("old bytes".length());
        assertThat(result.getMimeType()).isEqualTo("text/markdown");

        verify(fileStorage, never()).deleteFile(versionFile);

        ArgumentCaptor<AssetFileVersion> versionCaptor = ArgumentCaptor.forClass(AssetFileVersion.class);

        verify(versionRepository).save(versionCaptor.capture());

        assertThat(versionCaptor.getValue()
            .getFile()).isEqualTo(currentFile);
        assertThat(versionCaptor.getValue()
            .getVersionNumber()).isEqualTo(2);
    }

    @Test
    void testRestoreVersionRejectsForeignVersion() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setWorkspaceId(1L);

        AssetFileVersion foreignVersion = new AssetFileVersion();

        foreignVersion.setId(77L);
        foreignVersion.setAssetFileId(999L);

        when(service.findById(5L)).thenReturn(existing);
        when(versionRepository.findById(77L)).thenReturn(Optional.of(foreignVersion));

        assertThatThrownBy(() -> facade.restoreVersion(5L, 77L))
            .isInstanceOf(AssetFileNotFoundException.class);
    }

    @Test
    void testDeleteThrowsWhenAssetFileHasNoWorkspaceId() {
        AssetFile existing = new AssetFile();

        existing.setId(11L);
        existing.setFile(new FileEntry("x.txt", "asset-files/x.txt"));

        when(service.findById(11L)).thenReturn(existing);

        assertThatThrownBy(() -> facade.delete(11L))
            .isInstanceOf(AssetFileNotFoundException.class);

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testDeletePropagatesFailureFromSystemFacade() {
        AssetFile existing = new AssetFile();

        existing.setId(11L);
        existing.setWorkspaceId(1L);

        when(service.findById(11L)).thenReturn(existing);
        doThrow(new RuntimeException("storage down"))
            .when(assetFileSystemFacade)
            .deleteInWorkspace(11L, 1L);

        assertThatThrownBy(() -> facade.delete(11L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("storage down");
    }

    @Test
    void testEnablePublicLinkGeneratesTokenOnceAndIsIdempotent() {
        AssetFile existing = new AssetFile();

        existing.setId(5L);
        existing.setWorkspaceId(1L);

        when(service.findById(5L)).thenReturn(existing);
        when(service.update(any(AssetFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = facade.enablePublicLink(5L);

        assertThat(token).isNotBlank();
        assertThat(existing.getPublicLinkToken()).isEqualTo(token);

        assertThat(facade.enablePublicLink(5L)).isEqualTo(token);

        verify(service, times(1)).update(any(AssetFile.class));
    }

    @Test
    void testEnablePublicLinkRejectedWhenKillSwitchOff() {
        sharingProperties = new AutomationAssetFileSharingProperties(false);

        facade = newFacade();

        assertThatThrownBy(() -> facade.enablePublicLink(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("disabled");
    }
}
