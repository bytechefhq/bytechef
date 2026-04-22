/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;

/**
 * Workspace-agnostic CRUD + cancel for {@link AiObservabilityExportJob}. By-workspace listing + workspace association
 * live on the automation-side {@code WorkspaceAiObservabilityExportJobService}.
 *
 * @version ee
 */
public interface AiObservabilityExportJobService {

    AiObservabilityExportJob create(AiObservabilityExportJob exportJob);

    AiObservabilityExportJob getExportJob(long id);

    AiObservabilityExportJob update(AiObservabilityExportJob exportJob);

    AiObservabilityExportJob cancel(long id);
}
