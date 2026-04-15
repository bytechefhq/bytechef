/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.ee.automation.configuration.facade.ConnectionReassignmentFacade;
import com.bytechef.ee.automation.configuration.facade.ConnectionReassignmentFacade.AffectedWorkflow;
import com.bytechef.ee.automation.configuration.facade.ConnectionReassignmentFacade.ConnectionReassignmentItem;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ConnectionReassignmentGraphQlController {

    private final ConnectionReassignmentFacade connectionReassignmentFacade;

    @SuppressFBWarnings("EI")
    public ConnectionReassignmentGraphQlController(ConnectionReassignmentFacade connectionReassignmentFacade) {
        this.connectionReassignmentFacade = connectionReassignmentFacade;
    }

    @QueryMapping(name = "unresolvedConnections")
    public List<ConnectionReassignmentItem> unresolvedConnections(
        @Argument long workspaceId, @Argument String userLogin) {

        return connectionReassignmentFacade.getUnresolvedConnections(workspaceId, userLogin);
    }

    @QueryMapping(name = "affectedWorkflows")
    public List<AffectedWorkflow> affectedWorkflows(@Argument long workspaceId, @Argument String userLogin) {
        return connectionReassignmentFacade.getAffectedWorkflows(workspaceId, userLogin);
    }

    @MutationMapping(name = "markConnectionsPendingReassignment")
    public BulkReassignResultDTO markConnectionsPendingReassignment(
        @Argument long workspaceId, @Argument String userLogin) {

        // Routes to the ADMIN-guarded facade variant. The unguarded markConnectionsPendingReassignment is reserved
        // for WorkspaceUserRemovalListener's non-admin system path.
        return connectionReassignmentFacade.markConnectionsPendingReassignmentAsAdmin(workspaceId, userLogin);
    }

    @MutationMapping(name = "reassignConnection")
    public Boolean reassignConnection(
        @Argument long workspaceId, @Argument long connectionId, @Argument String newOwnerLogin) {

        connectionReassignmentFacade.reassignConnection(workspaceId, connectionId, newOwnerLogin);

        return true;
    }

    @MutationMapping(name = "reassignAllConnections")
    public Boolean reassignAllConnections(
        @Argument long workspaceId, @Argument String userLogin, @Argument String newOwnerLogin) {

        connectionReassignmentFacade.reassignAllConnections(workspaceId, userLogin, newOwnerLogin);

        return true;
    }
}
