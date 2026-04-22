/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityExportJobService {

    AiObservabilityExportJob create(AiObservabilityExportJob exportJob);

    AiObservabilityExportJob getExportJob(long id);

    List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId);

    AiObservabilityExportJob update(AiObservabilityExportJob exportJob);

    AiObservabilityExportJob cancel(long id);
}
