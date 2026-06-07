/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;

/**
 * Facade entry point for managing Context Store sources within a workspace. Each source owns an auto-generated workflow
 * of shape {@code [schedule.cronTrigger] -> [data-stream.stream(SOURCE=<component>.<itemReader>,
 * DESTINATION=contextStore.writeToReplica)]} that Atlas runs on its existing schedule + Worker dispatch infrastructure.
 *
 * <p>
 * The facade encapsulates the workspace-aware control-plane plumbing: persisting the source row, the entity rows, the
 * workflow definition, the workspace-relation row, and the {@code ProjectDeploymentWorkflow} that wires the workflow
 * into the workspace's deployment surface. Workspace-internal projects/deployments are auto-created on first use and
 * reused for subsequent sources.
 *
 * <p>
 * The {@code workspaceId} argument is the authoritative scoping signal — every method takes it explicitly so the facade
 * does not have to resolve it from the source row indirectly. Callers (the GraphQL controller and the AI Hub tool
 * callbacks) read {@code workspaceId} from the chat / request context and pass it through.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreSourceFacade {

    /**
     * Creates a new Context Store source together with its entities, registers it under the given workspace,
     * auto-generates the source-bound workflow, and triggers an initial sync.
     */
    ContextStoreSource create(Long workspaceId, CreateContextStoreSourceInput input);

    /**
     * Updates a Context Store source. A cadence change mutates the workflow's trigger cron parameter only; the rest of
     * the workflow definition is preserved. An enabled flag change toggles the workflow's
     * {@code ProjectDeploymentWorkflow}.
     */
    ContextStoreSource update(Long workspaceId, Long sourceId, UpdateContextStoreSourceInput input);

    /**
     * Deletes a Context Store source, its auto-generated workflow, the corresponding {@code
     * ProjectDeploymentWorkflow}, the workspace-relation row, and (via cascade) all entities/records/index rows.
     */
    void delete(Long workspaceId, Long sourceId);

    /**
     * Manually triggers a sync run by creating a {@code Job} against the source's workflow.
     *
     * @return the created job's ID (Atlas job, not Spring Batch job execution).
     */
    long refreshNow(Long workspaceId, Long sourceId);

    /**
     * Toggles the enabled flag on the source and the corresponding {@code ProjectDeploymentWorkflow} (which controls
     * trigger activation in the scheduler).
     */
    void setEnabled(Long workspaceId, Long sourceId, boolean enabled);
}
