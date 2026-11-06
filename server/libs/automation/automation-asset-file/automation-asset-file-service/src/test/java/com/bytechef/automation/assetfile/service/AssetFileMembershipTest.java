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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.cleanup.AssetFileOrphanBlobRecorder;
import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.repository.AssetFileVersionRepository;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AssetFileMembershipTest {

    private static final long USER_ID = 1L;
    private static final Long WORKSPACE_ID = 1L;
    private static final Long OTHER_WORKSPACE_ID = 2L;
    private static final Long ASSET_FILE_ID = 5L;
    private static final int ENVIRONMENT = 0;

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

    private AssetFileFacade assetFileFacade;

    @BeforeEach
    void setUp() {
        AutomationAssetFileQuotaProperties quota = new AutomationAssetFileQuotaProperties(
            26_214_400L, 1_073_741_824L, 1_048_576L, 10);
        AutomationAssetFileSharingProperties sharingProperties = new AutomationAssetFileSharingProperties(true);

        AssetFileWriteSupport writeSupport = new AssetFileWriteSupport(
            service, fileStorage, metrics, orphanBlobRecorder, quota);

        assetFileFacade = new AssetFileFacadeImpl(
            service, fileStorage, metrics, versionRepository, assetFileSystemFacade, writeSupport,
            quota, sharingProperties, userService, workspaceFacade);
    }

    @Test
    void testCreateFromUploadRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .as("the caller is asserting membership of a workspace, so it is told plainly that the assertion failed")
            .isThrownBy(() -> assetFileFacade.createFromUpload(
                OTHER_WORKSPACE_ID, ENVIRONMENT, "f.txt", "text/plain", InputStream.nullInputStream()));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testCreateFromAiRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> assetFileFacade.createFromAi(
                OTHER_WORKSPACE_ID, ENVIRONMENT, "note.md", "text/markdown", "content", null, null, (short) 1,
                "prompt"));

        verifyNoInteractions(service);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void testCreateBinaryFromAiRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> assetFileFacade.createBinaryFromAi(
                OTHER_WORKSPACE_ID, ENVIRONMENT, "icon.png", "image/png", new byte[] {
                    1, 2, 3
                }, null, null,
                (short) 1, "prompt"));

        verifyNoInteractions(service);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void testFindAllByWorkspaceIdAndEnvironmentRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> assetFileFacade.findAllByWorkspaceIdAndEnvironment(
                OTHER_WORKSPACE_ID, ENVIRONMENT, List.of()));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testFindByIdInWorkspaceRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> assetFileFacade.findByIdInWorkspace(5L, OTHER_WORKSPACE_ID));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testCloneToEnvironmentRefusesAWorkspaceTheUserIsNotAMemberOf() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AccessDeniedException.class)
            .as("the membership check runs before target-environment validation, so an invalid ordinal here still "
                + "surfaces as denial rather than IllegalArgumentException")
            .isThrownBy(() -> assetFileFacade.cloneToEnvironment(5L, OTHER_WORKSPACE_ID, -1, null));

        verifyNoInteractions(assetFileSystemFacade);
        verifyNoInteractions(service);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void testCreateFromUploadRefusesANullWorkspaceId() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .as("a null workspace id can never be a member of, regardless of who is calling")
            .isThrownBy(() -> assetFileFacade.createFromUpload(
                null, ENVIRONMENT, "f.txt", "text/plain", InputStream.nullInputStream()));

        verifyNoInteractions(userService);
        verifyNoInteractions(workspaceFacade);
        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testFindAllByWorkspaceIdAndEnvironmentRefusesAnUnauthenticatedCaller() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.empty());

        assertThatExceptionOfType(AccessDeniedException.class)
            .as("with no current user there is no membership to assert")
            .isThrownBy(() -> assetFileFacade.findAllByWorkspaceIdAndEnvironment(
                WORKSPACE_ID, ENVIRONMENT, List.of()));

        verifyNoInteractions(workspaceFacade);
        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testDeleteRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .as("by-id failures stay 404-shaped so a probe cannot confirm the id exists in another workspace")
            .isThrownBy(() -> assetFileFacade.delete(ASSET_FILE_ID));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testDownloadContentRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.downloadContent(ASSET_FILE_ID));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testFindByIdRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.findById(ASSET_FILE_ID));
    }

    @Test
    void testRenameRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.rename(ASSET_FILE_ID, "renamed.txt"));

        verifyNoInteractions(assetFileSystemFacade);
    }

    @Test
    void testUpdateDescriptionRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.updateDescription(ASSET_FILE_ID, "new description"));

        verify(service, never()).update(any());
    }

    @Test
    void testGetVersionsRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.getVersions(ASSET_FILE_ID));

        verifyNoInteractions(versionRepository);
    }

    @Test
    void testRestoreVersionRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.restoreVersion(ASSET_FILE_ID, 99L));

        verify(service, never()).update(any());

        verifyNoInteractions(versionRepository);
    }

    @Test
    void testEnablePublicLinkRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.enablePublicLink(ASSET_FILE_ID));

        verify(service, never()).update(any());
    }

    @Test
    void testDisablePublicLinkRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.disablePublicLink(ASSET_FILE_ID));

        verify(service, never()).update(any());
    }

    @Test
    void testUpdateContentRefusesAFileInAWorkspaceTheUserIsNotAMemberOf() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE_ID);
        assetFile.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(service.findById(ASSET_FILE_ID)).thenReturn(assetFile);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user(USER_ID)));
        when(workspaceFacade.getUserWorkspaces(USER_ID)).thenReturn(List.of(workspace(WORKSPACE_ID)));

        assertThatExceptionOfType(AssetFileNotFoundException.class)
            .isThrownBy(() -> assetFileFacade.updateContent(
                ASSET_FILE_ID, "text/plain", InputStream.nullInputStream()));

        verifyNoInteractions(assetFileSystemFacade);
    }

    private User user(Long id) {
        User user = new User();

        user.setId(id);

        return user;
    }

    private Workspace workspace(Long id) {
        return new Workspace(id, "workspace-" + id);
    }
}
