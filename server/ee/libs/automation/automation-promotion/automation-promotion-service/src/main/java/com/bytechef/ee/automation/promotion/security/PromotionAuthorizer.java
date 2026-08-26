/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.security;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Resolves the owning project or workspace of a promotable resource, for the {@code @PreAuthorize} expressions on
 * {@link com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler} implementations to key their
 * permission check on.
 *
 * <p>
 * A handler deliberately does NOT resolve its own owner id from inside its own guard: a
 * {@code @PreAuthorize("hasPermission(@thisVeryBean.something(#id), ...)")} expression makes the guarded bean call
 * itself through its own security proxy while that proxy is still evaluating the guard. Delegating to a separate bean
 * is the shape the codebase already uses — see {@code WorkspaceConnectionFacadeImpl}'s
 * {@code @PreAuthorize("@permissionService.isResourceOwner(...)")}.
 * </p>
 *
 * <p>
 * <b>An unresolvable id is an {@link AccessDeniedException}, never a typed "not found".</b> Every lookup here runs
 * BEFORE any authorization verdict exists, so at this point the bean cannot tell "no such resource" from "a resource
 * this caller may not see" — and a caller who can distinguish the two has an oracle for enumerating ids outside their
 * workspace. That is the same disclosure argument that made {@code ConnectionEnvironmentMapper#validate} reject an
 * invisible target and a nonexistent one with one indistinguishable message. The cost is that a genuine typo or a
 * concurrently deleted resource is reported as 403 rather than as
 * {@code EnvironmentPromotionErrorType.SOURCE_NOT_FOUND}; that is the correct trade, because the alternative leaks to
 * everyone in order to be clearer to the few who were entitled to the resource anyway.
 * </p>
 *
 * <p>
 * <b>Extending this for another resource type:</b> add one {@code <ownerKind>Of<ResourceKind>(long)} method returning
 * the raw id the permission check keys on, plus whatever read-only service it needs as a constructor collaborator. The
 * method must (1) read through a {@code fetch…} form and convert an absent result into {@link AccessDeniedException},
 * exactly as {@link #projectIdOfApiCollection} does — a {@code get…} form that throws {@code NoSuchElementException}
 * escapes guard evaluation as a 500 — (2) stay side-effect free, and (3) never call a method that is itself
 * {@code @PreAuthorize}-guarded, or the guard recurses.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("promotionAuthorizer")
@ConditionalOnEEVersion
public class PromotionAuthorizer {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public PromotionAuthorizer(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    /**
     * @param apiCollectionId the id of the API collection being promoted
     * @return the id of the project the collection's deployment runs, which is what {@code DEPLOYMENT_PUSH} is checked
     *         against
     * @throws AccessDeniedException if no such API collection exists — see the class note on why this is not a typed
     *                               "not found"
     */
    public long projectIdOfApiCollection(long apiCollectionId) {
        ApiCollection apiCollection = apiCollectionService.fetchApiCollection(apiCollectionId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        // The deployment is reached through a NOT NULL foreign key of a row that was just found, so an absent one is
        // broken data rather than a caller-supplied id, and is deliberately NOT masked as an authorization failure.
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(apiCollection.getProjectDeploymentId(), "projectDeploymentId"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }

    /**
     * @param mcpServerId the id of the MCP server being promoted
     * @return the id of the workspace the server is assigned to, which is what {@code MCP_CREATE} is checked against
     * @throws AccessDeniedException if no such MCP server exists, or it is assigned to no workspace — see the class
     *                               note on why neither is a typed "not found". An unassigned server is deliberately
     *                               folded into the same outcome: {@code workspace_mcp_server} is the only thing that
     *                               scopes an MCP server to a caller, so a server without a row has no workspace whose
     *                               permissions could grant access to it.
     */
    public long workspaceIdOfMcpServer(long mcpServerId) {
        return workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));
    }

    /**
     * @param projectDeploymentId the id of the project deployment being promoted
     * @return the id of the project the deployment belongs to, which is what {@code DEPLOYMENT_PUSH} is checked against
     * @throws AccessDeniedException if no such project deployment exists — see the class note on why this is not a
     *                               typed "not found"
     */
    public long projectIdOfProjectDeployment(long projectDeploymentId) {
        ProjectDeployment projectDeployment = projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        return Objects.requireNonNull(projectDeployment.getProjectId(), "projectId");
    }
}
