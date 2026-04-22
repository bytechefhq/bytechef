/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import java.util.List;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilityExportJobService {

    AiObservabilityExportJob createInWorkspace(AiObservabilityExportJob exportJob, long workspaceId);

    List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId);

    Long getWorkspaceId(long exportJobId);
}
