/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetItemView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetVersionView;
import com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto.AiEvalDatasetView;
import java.util.List;

/**
 * Facade for the operator console's read-only dataset queries. Hosts their workspace-boundary guards so they apply to
 * every caller of the facade rather than only the GraphQL entry point, and keeps them off the shared
 * {@code AiEvalDatasetService} / {@code AiEvalDatasetVersionService} which dataset curation relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiEvalDatasetQueryFacade {

    List<AiEvalDatasetView> getAiEvalDatasets(Long workspaceId);

    List<AiEvalDatasetVersionView> getAiEvalDatasetVersions(Long datasetId);

    List<AiEvalDatasetItemView> getAiEvalDatasetItems(Long versionId);
}
