/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityExportJobFacade;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportFormat;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJobType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportScope;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityExportJobGraphQlController {

    private final AiObservabilityExportJobFacade aiObservabilityExportJobFacade;

    @SuppressFBWarnings("EI")
    AiObservabilityExportJobGraphQlController(AiObservabilityExportJobFacade aiObservabilityExportJobFacade) {
        this.aiObservabilityExportJobFacade = aiObservabilityExportJobFacade;
    }

    @QueryMapping
    public AiObservabilityExportJob aiObservabilityExportJob(@Argument long id) {
        // Authorization (ROLE_ADMIN) is enforced on AiObservabilityExportJobFacade so it protects every caller of the
        // facade, not just this GraphQL entry point.
        return aiObservabilityExportJobFacade.getExportJob(id);
    }

    @QueryMapping
    public List<AiObservabilityExportJob> aiObservabilityExportJobs(@Argument Long workspaceId) {
        // Authorization (workspace VIEWER role) is enforced on AiObservabilityExportJobFacade so it protects every
        // caller of the facade, not just this GraphQL entry point.
        return aiObservabilityExportJobFacade.getExportJobsByWorkspace(workspaceId);
    }

    @MutationMapping
    public AiObservabilityExportJob createAiObservabilityExportJob(
        @Argument Long workspaceId, @Argument Long projectId,
        @Argument AiObservabilityExportFormat format, @Argument AiObservabilityExportScope scope,
        @Argument String filters, @Argument AiObservabilityExportJobType type,
        @Argument String cronExpression) {

        // Authorization (ROLE_ADMIN) is enforced on AiObservabilityExportJobFacade so it protects every caller of the
        // facade, not just this GraphQL entry point.
        return aiObservabilityExportJobFacade.createExportJob(
            workspaceId, projectId, format, scope, filters, type, cronExpression);
    }

    @MutationMapping
    public AiObservabilityExportJob cancelAiObservabilityExportJob(@Argument long id) {
        // Authorization (ROLE_ADMIN) is enforced on AiObservabilityExportJobFacade so it protects every caller of the
        // facade, not just this GraphQL entry point.
        return aiObservabilityExportJobFacade.cancelExportJob(id);
    }
}
