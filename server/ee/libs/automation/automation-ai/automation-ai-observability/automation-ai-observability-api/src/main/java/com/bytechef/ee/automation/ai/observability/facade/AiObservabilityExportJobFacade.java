/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportFormat;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJobType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportScope;
import java.util.List;

/**
 * Facade for AI Observability export jobs. Hosts the authorization guards (admin for read/create/cancel, workspace
 * VIEWER role for the workspace listing) so they apply to every caller of the facade rather than only the GraphQL entry
 * point, and keeps them off the shared {@code AiObservabilityExportJobService} which the export executor relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiObservabilityExportJobFacade {

    AiObservabilityExportJob cancelExportJob(long id);

    AiObservabilityExportJob createExportJob(
        Long workspaceId, Long projectId, AiObservabilityExportFormat format, AiObservabilityExportScope scope,
        String filters, AiObservabilityExportJobType type, String cronExpression);

    AiObservabilityExportJob getExportJob(long id);

    List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId);
}
