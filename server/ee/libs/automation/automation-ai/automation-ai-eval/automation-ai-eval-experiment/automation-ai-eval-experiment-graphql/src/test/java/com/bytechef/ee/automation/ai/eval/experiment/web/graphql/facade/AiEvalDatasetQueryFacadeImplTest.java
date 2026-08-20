/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetItemView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetVersionView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetView;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins the workspace-boundary guards that moved off {@code AiEvalDatasetGraphQlController}'s method bodies onto this
 * facade. Each query MUST surface a {@link AiScoreWorkspaceBoundaryException} with a uniform message for both the "id
 * does not exist" and "id exists in a different workspace" cases — distinguishable error shapes would let workspace-A
 * enumerate workspace-B's dataset / version / item ids by probing message deltas. Mirrors the pattern on
 * {@code AiEvalExperimentQueryFacadeImplTest}; the depth-2 boundary on items (version → dataset → workspace) gets its
 * own test.
 *
 * <p>
 * The cross-workspace tests go red if the corresponding guard is deleted: each asserts a rejection where the unguarded
 * code would have returned rows.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalDatasetQueryFacadeImplTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long FOREIGN_WORKSPACE_ID = 99L;
    private static final long DATASET_ID = 10L;
    private static final long DATASET_VERSION_ID = 100L;
    private static final long DATASET_ITEM_ID_1 = 1001L;
    private static final long DATASET_ITEM_ID_2 = 1002L;

    @Mock
    private AiEvalDatasetService aiEvalDatasetService;

    @Mock
    private WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;

    @Mock
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @Mock
    private AiEvalDatasetItemService aiEvalDatasetItemService;

    @Mock
    private PermissionService permissionService;

    private AiEvalDatasetQueryFacade facade;

    @BeforeEach
    void setUp() {
        facade = new AiEvalDatasetQueryFacadeImpl(
            aiEvalDatasetService, workspaceAiEvalDatasetService, aiEvalDatasetVersionService,
            aiEvalDatasetItemService, permissionService);

        // Default to allowing access; cross-workspace tests override per-workspace. Lenient because
        // the null-id paths never reach the permission check.
        lenient()
            .when(permissionService.hasWorkspaceRole(anyLong(), eq("VIEWER")))
            .thenReturn(true);
    }

    @Test
    void testAiEvalDatasetsReturnsListForWorkspace() {
        AiEvalDataset datasetOne = newDataset(DATASET_ID, "qa-set", WORKSPACE_ID);
        AiEvalDataset datasetTwo = newDataset(DATASET_ID + 1, "regression-set", WORKSPACE_ID);

        when(workspaceAiEvalDatasetService.findAllByWorkspace(WORKSPACE_ID))
            .thenReturn(List.of(datasetOne, datasetTwo));

        List<AiEvalDatasetView> views = facade.getAiEvalDatasets(WORKSPACE_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0)
            .id()).isEqualTo(DATASET_ID);
        assertThat(views.get(0)
            .name()).isEqualTo("qa-set");
        assertThat(views.get(1)
            .id()).isEqualTo(DATASET_ID + 1);
    }

    @Test
    void testAiEvalDatasetsReturnsEmptyListWhenWorkspaceIdIsNull() {
        List<AiEvalDatasetView> views = facade.getAiEvalDatasets(null);

        assertThat(views).isEmpty();
    }

    @Test
    void testAiEvalDatasetsRejectsCallerWithoutWorkspaceViewerRole() {
        when(permissionService.hasWorkspaceRole(FOREIGN_WORKSPACE_ID, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getAiEvalDatasets(FOREIGN_WORKSPACE_ID))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("workspace " + FOREIGN_WORKSPACE_ID);
    }

    @Test
    void testAiEvalDatasetVersionsReturnsListWithItemCount() {
        AiEvalDataset dataset = newDataset(DATASET_ID, "qa-set", WORKSPACE_ID);
        AiEvalDatasetVersion versionOne = newVersion(DATASET_VERSION_ID, DATASET_ID, "v1", true);
        AiEvalDatasetVersion versionTwo = newVersion(DATASET_VERSION_ID + 1, DATASET_ID, null, false);

        when(aiEvalDatasetService.getDataset(DATASET_ID)).thenReturn(dataset);
        when(aiEvalDatasetVersionService.findAllByDataset(DATASET_ID)).thenReturn(List.of(versionOne, versionTwo));
        when(aiEvalDatasetItemService.countByVersion(DATASET_VERSION_ID)).thenReturn(5L);
        when(aiEvalDatasetItemService.countByVersion(DATASET_VERSION_ID + 1)).thenReturn(2L);

        List<AiEvalDatasetVersionView> views = facade.getAiEvalDatasetVersions(DATASET_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0)
            .id()).isEqualTo(DATASET_VERSION_ID);
        assertThat(views.get(0)
            .itemCount()).isEqualTo(5L);
        assertThat(views.get(0)
            .frozen()).isTrue();
        assertThat(views.get(1)
            .id()).isEqualTo(DATASET_VERSION_ID + 1);
        assertThat(views.get(1)
            .itemCount()).isEqualTo(2L);
        assertThat(views.get(1)
            .frozen()).isFalse();
    }

    @Test
    void testAiEvalDatasetVersionsReturnsEmptyListWhenDatasetIdIsNull() {
        List<AiEvalDatasetVersionView> views = facade.getAiEvalDatasetVersions(null);

        assertThat(views).isEmpty();
    }

    @Test
    void testAiEvalDatasetVersionsCollapsesNotFoundOntoBoundaryException() {
        long missingId = 9_999L;

        when(aiEvalDatasetService.getDataset(missingId))
            .thenThrow(new IllegalArgumentException("Dataset " + missingId + " not found"));

        assertThatThrownBy(() -> facade.getAiEvalDatasetVersions(missingId))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("dataset " + missingId);
    }

    @Test
    void testAiEvalDatasetVersionsRejectsCrossWorkspaceDataset() {
        AiEvalDataset foreignDataset = newDataset(DATASET_ID, "qa-set", FOREIGN_WORKSPACE_ID);

        when(aiEvalDatasetService.getDataset(DATASET_ID)).thenReturn(foreignDataset);
        when(permissionService.hasWorkspaceRole(FOREIGN_WORKSPACE_ID, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getAiEvalDatasetVersions(DATASET_ID))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("dataset " + DATASET_ID)
            .hasMessageNotContaining("workspace " + FOREIGN_WORKSPACE_ID);
    }

    @Test
    void testAiEvalDatasetVersionsReturnsSameErrorShapeForNotFoundAndCrossWorkspace() {
        long missingId = 9_999L;
        long foreignId = DATASET_ID;

        AiEvalDataset foreignDataset = newDataset(foreignId, "qa-set", FOREIGN_WORKSPACE_ID);

        when(aiEvalDatasetService.getDataset(missingId))
            .thenThrow(new IllegalArgumentException("Dataset " + missingId + " not found"));
        when(aiEvalDatasetService.getDataset(foreignId)).thenReturn(foreignDataset);
        when(permissionService.hasWorkspaceRole(FOREIGN_WORKSPACE_ID, "VIEWER")).thenReturn(false);

        Throwable missingThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalDatasetVersions(missingId));
        Throwable foreignThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalDatasetVersions(foreignId));

        assertThat(missingThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(foreignThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(missingThrown.getMessage()
            .replace(Long.toString(missingId), "<id>"))
                .isEqualTo(foreignThrown.getMessage()
                    .replace(Long.toString(foreignId), "<id>"));
    }

    @Test
    void testAiEvalDatasetItemsReturnsListForVisibleVersion() {
        AiEvalDataset dataset = newDataset(DATASET_ID, "qa-set", WORKSPACE_ID);
        AiEvalDatasetVersion version = newVersion(DATASET_VERSION_ID, DATASET_ID, "v1", false);

        AiEvalDatasetItem itemOne = newItem(DATASET_ITEM_ID_1, DATASET_VERSION_ID, "{\"prompt\":\"a\"}");
        AiEvalDatasetItem itemTwo = newItem(DATASET_ITEM_ID_2, DATASET_VERSION_ID, "{\"prompt\":\"b\"}");

        when(aiEvalDatasetVersionService.getVersion(DATASET_VERSION_ID)).thenReturn(version);
        when(aiEvalDatasetService.getDataset(DATASET_ID)).thenReturn(dataset);
        when(aiEvalDatasetItemService.getItemsByVersion(DATASET_VERSION_ID)).thenReturn(List.of(itemOne, itemTwo));

        List<AiEvalDatasetItemView> views = facade.getAiEvalDatasetItems(DATASET_VERSION_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0)
            .id()).isEqualTo(DATASET_ITEM_ID_1);
        assertThat(views.get(0)
            .input()).isEqualTo("{\"prompt\":\"a\"}");
        assertThat(views.get(1)
            .id()).isEqualTo(DATASET_ITEM_ID_2);
    }

    @Test
    void testAiEvalDatasetItemsReturnsEmptyListWhenVersionIdIsNull() {
        List<AiEvalDatasetItemView> views = facade.getAiEvalDatasetItems(null);

        assertThat(views).isEmpty();
    }

    @Test
    void testAiEvalDatasetItemsCollapsesMissingVersionOntoBoundaryException() {
        long missingId = 9_999L;

        when(aiEvalDatasetVersionService.getVersion(missingId))
            .thenThrow(new IllegalArgumentException("Version " + missingId + " not found"));

        assertThatThrownBy(() -> facade.getAiEvalDatasetItems(missingId))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("dataset version " + missingId);
    }

    @Test
    void testAiEvalDatasetItemsRejectsCrossWorkspaceViaParentDataset() {
        // Depth-2 boundary check: the version itself does not carry a workspace id, so the boundary check
        // walks up to the parent dataset. A regression that drops the parent-dataset lookup would surface
        // here as a permitted cross-workspace listing.
        AiEvalDataset foreignDataset = newDataset(DATASET_ID, "qa-set", FOREIGN_WORKSPACE_ID);
        AiEvalDatasetVersion version = newVersion(DATASET_VERSION_ID, DATASET_ID, "v1", false);

        when(aiEvalDatasetVersionService.getVersion(DATASET_VERSION_ID)).thenReturn(version);
        when(aiEvalDatasetService.getDataset(DATASET_ID)).thenReturn(foreignDataset);
        when(permissionService.hasWorkspaceRole(FOREIGN_WORKSPACE_ID, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getAiEvalDatasetItems(DATASET_VERSION_ID))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("dataset version " + DATASET_VERSION_ID)
            .hasMessageNotContaining("workspace " + FOREIGN_WORKSPACE_ID);
    }

    @Test
    void testAiEvalDatasetItemsReturnsSameErrorShapeForNotFoundAndCrossWorkspace() {
        long missingId = 9_999L;
        long foreignId = DATASET_VERSION_ID;

        AiEvalDataset foreignDataset = newDataset(DATASET_ID, "qa-set", FOREIGN_WORKSPACE_ID);
        AiEvalDatasetVersion foreignVersion = newVersion(foreignId, DATASET_ID, "v1", false);

        when(aiEvalDatasetVersionService.getVersion(missingId))
            .thenThrow(new IllegalArgumentException("Version " + missingId + " not found"));
        when(aiEvalDatasetVersionService.getVersion(foreignId)).thenReturn(foreignVersion);
        when(aiEvalDatasetService.getDataset(DATASET_ID)).thenReturn(foreignDataset);
        when(permissionService.hasWorkspaceRole(FOREIGN_WORKSPACE_ID, "VIEWER")).thenReturn(false);

        Throwable missingThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalDatasetItems(missingId));
        Throwable foreignThrown =
            org.assertj.core.api.Assertions.catchThrowable(() -> facade.getAiEvalDatasetItems(foreignId));

        assertThat(missingThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(foreignThrown).isInstanceOf(AiScoreWorkspaceBoundaryException.class);
        assertThat(missingThrown.getMessage()
            .replace(Long.toString(missingId), "<id>"))
                .isEqualTo(foreignThrown.getMessage()
                    .replace(Long.toString(foreignId), "<id>"));
    }

    private AiEvalDataset newDataset(long id, String name, long workspaceId) {
        AiEvalDataset dataset = new AiEvalDataset(name);

        ReflectionTestUtils.setField(dataset, "id", id);

        // Stub the workspace lookup the facade uses to enforce the boundary check.
        lenient().when(workspaceAiEvalDatasetService.getWorkspaceId(id))
            .thenReturn(workspaceId);

        return dataset;
    }

    private static AiEvalDatasetVersion newVersion(long id, long datasetId, String label, boolean frozen) {
        // versionNumber is an internal monotonic counter; tests use the id as a stand-in to keep
        // each fixture's number unique without a separate counter parameter.
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(datasetId, (int) id);

        if (label != null) {
            version.setLabel(label);
        }

        ReflectionTestUtils.setField(version, "id", id);

        if (frozen) {
            version.freeze();
        }

        return version;
    }

    private static AiEvalDatasetItem newItem(long id, long versionId, String input) {
        AiEvalDatasetItem item = AiEvalDatasetItem.newItem(DATASET_ID, versionId, input, null, null);

        ReflectionTestUtils.setField(item, "id", id);

        return item;
    }
}
