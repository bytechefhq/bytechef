/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Set;

/**
 * Owns the lifecycle of a connected user's reference to a shared catalog code workflow: provisioning on first use,
 * per-user connection auto-wiring, enable/disable, deletion, and flagging references whose catalog workflow was removed
 * on redeploy ("dangling"). Also serves as the read seam for callers that need every
 * {@link ConnectedUserProjectWorkflow} row (both reference-mode and copy-mode) belonging to a connected user, so those
 * callers never need to depend on the repository directly.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserCodeWorkflowReferenceFacade {

    void deleteReference(String externalUserId, String catalogWorkflowUuid, Environment environment);

    void enableReference(String externalUserId, String catalogWorkflowUuid, boolean enable, Environment environment);

    /**
     * Returns every {@link ConnectedUserProjectWorkflow} row belonging to the connected user, across all of their
     * {@code ConnectedUserProject}s, regardless of whether the row is reference-mode
     * ({@code catalogWorkflowUuid != null}) or copy-mode ({@code projectWorkflowId != null}).
     */
    List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId);

    /**
     * @throws MissingConnectionException if the reference cannot be auto-wired because a component it uses has no
     *                                    matching connection for the connected user. The reference is still created,
     *                                    left disabled.
     */
    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment);

    /**
     * A reference dangles iff its {@code catalog_workflow_uuid} was served by this catalog project in the previous
     * deploy and is no longer served by the current one. Both sets are scoped to a SINGLE catalog project (uuids the
     * project served before vs. now), so a redeploy of one catalog project can never dangle a reference to a different
     * catalog project -- there is deliberately no repository-wide "not in the current set" scan.
     */
    void markDanglingReferences(
        long catalogProjectId, Set<String> previousCatalogWorkflowUuids, Set<String> currentCatalogWorkflowUuids);
}
