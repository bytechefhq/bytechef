/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetItemView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetVersionView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalDatasetQueryFacade;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins that every dataset query reaches its rows through the facade layer, which is where this codebase puts
 * authorization. The workspace-boundary guards themselves are pinned by {@code AiEvalDatasetQueryFacadeImplTest}.
 *
 * <p>
 * The controller carries no gate of its own and is not meant to. Asserting the delegations is not enough on its own:
 * {@code testControllerHoldsNoAuthorizationOrDataCollaborators} is what makes a revert to a locally assembled listing
 * with a locally written {@code permissionService} check fail here rather than pass, since such a listing has to
 * reintroduce those collaborators as fields.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetGraphQlControllerTest {

    private static final long DATASET_ID = 10L;
    private static final long DATASET_VERSION_ID = 100L;
    private static final long WORKSPACE_ID = 1L;

    private final AiEvalDatasetQueryFacade aiEvalDatasetQueryFacade = mock(AiEvalDatasetQueryFacade.class);

    private final AiEvalDatasetGraphQlController aiEvalDatasetGraphQlController =
        new AiEvalDatasetGraphQlController(aiEvalDatasetQueryFacade);

    @Test
    void testAiEvalDatasetsReadsThroughTheGuardedFacade() {
        List<AiEvalDatasetView> expected = List.of();

        when(aiEvalDatasetQueryFacade.getAiEvalDatasets(WORKSPACE_ID)).thenReturn(expected);

        assertThat(aiEvalDatasetGraphQlController.aiEvalDatasets(WORKSPACE_ID)).isSameAs(expected);

        verify(aiEvalDatasetQueryFacade).getAiEvalDatasets(WORKSPACE_ID);
    }

    @Test
    void testAiEvalDatasetVersionsReadsThroughTheGuardedFacade() {
        List<AiEvalDatasetVersionView> expected = List.of();

        when(aiEvalDatasetQueryFacade.getAiEvalDatasetVersions(DATASET_ID)).thenReturn(expected);

        assertThat(aiEvalDatasetGraphQlController.aiEvalDatasetVersions(DATASET_ID)).isSameAs(expected);

        verify(aiEvalDatasetQueryFacade).getAiEvalDatasetVersions(DATASET_ID);
    }

    @Test
    void testAiEvalDatasetItemsReadsThroughTheGuardedFacade() {
        List<AiEvalDatasetItemView> expected = List.of();

        when(aiEvalDatasetQueryFacade.getAiEvalDatasetItems(DATASET_VERSION_ID)).thenReturn(expected);

        assertThat(aiEvalDatasetGraphQlController.aiEvalDatasetItems(DATASET_VERSION_ID)).isSameAs(expected);

        verify(aiEvalDatasetQueryFacade).getAiEvalDatasetItems(DATASET_VERSION_ID);
    }

    @Test
    void testControllerHoldsNoAuthorizationOrDataCollaborators() {
        assertThat(Arrays.stream(AiEvalDatasetGraphQlController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization belongs on the facade; the controller must assemble nothing out of raw services")
                .doesNotContain(
                    PermissionService.class, AiEvalDatasetService.class, AiEvalDatasetVersionService.class,
                    AiEvalDatasetItemService.class, WorkspaceAiEvalDatasetService.class);
    }
}
