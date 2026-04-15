/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade.AffectedWorkflow;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade.ConnectionReassignmentItem;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectionReassignmentGraphQlController {

    private final ConnectionReassignmentFacade connectionReassignmentFacade;

    @SuppressFBWarnings("EI")
    public ConnectionReassignmentGraphQlController(ConnectionReassignmentFacade connectionReassignmentFacade) {
        this.connectionReassignmentFacade = connectionReassignmentFacade;
    }

    @QueryMapping(name = "unresolvedConnections")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ConnectionReassignmentItem> unresolvedConnections(
        @Argument long workspaceId, @Argument String userLogin) {

        return connectionReassignmentFacade.getUnresolvedConnections(workspaceId, userLogin);
    }

    @QueryMapping(name = "affectedWorkflows")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AffectedWorkflow> affectedWorkflows(@Argument long workspaceId, @Argument String userLogin) {
        return connectionReassignmentFacade.getAffectedWorkflows(workspaceId, userLogin);
    }

    @MutationMapping(name = "markConnectionsPendingReassignment")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public BulkReassignResultDTO markConnectionsPendingReassignment(
        @Argument long workspaceId, @Argument String userLogin) {

        return connectionReassignmentFacade.markConnectionsPendingReassignment(workspaceId, userLogin);
    }

    @MutationMapping(name = "reassignConnection")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean reassignConnection(
        @Argument long workspaceId, @Argument long connectionId, @Argument String newOwnerLogin) {

        connectionReassignmentFacade.reassignConnection(workspaceId, connectionId, newOwnerLogin);

        return true;
    }

    @MutationMapping(name = "reassignAllConnections")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean reassignAllConnections(
        @Argument long workspaceId, @Argument String userLogin, @Argument String newOwnerLogin) {

        connectionReassignmentFacade.reassignAllConnections(workspaceId, userLogin, newOwnerLogin);

        return true;
    }
}
