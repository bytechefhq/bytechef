/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetItemView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetVersionView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade.AiEvalDatasetQueryFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Read-only GraphQL surface for the operator console's Datasets tab. Lists workspace-scoped datasets, drills into their
 * versions, and lists items per version. All write operations remain on the public REST surface
 * ({@code AiEvalDatasetController}) — the operator console is a read-only viewer; dataset curation happens via SDK /
 * CI.
 *
 * <p>
 * Co-located in the experiment-graphql module rather than a sibling dataset-graphql module because the operator console
 * drills from a dataset into the experiments that ran against it, and pinning dataset GraphQL alongside experiment
 * GraphQL keeps that cross-resource lookup query path on the same module classpath.
 *
 * <p>
 * Authorization lives on {@link AiEvalDatasetQueryFacade}, which carries the workspace-boundary guard for each of these
 * queries &mdash; the API facade is this codebase's authorization layer, and this controller carries no gate of its
 * own. Those guards used to be written out in these method bodies, where they were invisible to any audit scanning for
 * {@code @PreAuthorize}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiEvalDatasetGraphQlController {

    private final AiEvalDatasetQueryFacade aiEvalDatasetQueryFacade;

    @SuppressFBWarnings("EI")
    AiEvalDatasetGraphQlController(AiEvalDatasetQueryFacade aiEvalDatasetQueryFacade) {
        this.aiEvalDatasetQueryFacade = aiEvalDatasetQueryFacade;
    }

    @QueryMapping
    public List<AiEvalDatasetView> aiEvalDatasets(@Argument Long workspaceId) {
        return aiEvalDatasetQueryFacade.getAiEvalDatasets(workspaceId);
    }

    @QueryMapping
    public List<AiEvalDatasetVersionView> aiEvalDatasetVersions(@Argument Long datasetId) {
        return aiEvalDatasetQueryFacade.getAiEvalDatasetVersions(datasetId);
    }

    @QueryMapping
    public List<AiEvalDatasetItemView> aiEvalDatasetItems(@Argument Long versionId) {
        return aiEvalDatasetQueryFacade.getAiEvalDatasetItems(versionId);
    }
}
