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
import com.bytechef.automation.configuration.dto.BulkPromoteResultDTO;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectionGraphQlController {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionGraphQlController(WorkspaceConnectionFacade workspaceConnectionFacade) {
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @MutationMapping(name = "disconnectConnection")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean disconnectConnection(@Argument long connectionId) {
        workspaceConnectionFacade.disconnectConnection(connectionId);

        return true;
    }

    @MutationMapping(name = "shareConnectionToProject")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean shareConnectionToProject(
        @Argument long workspaceId, @Argument long connectionId, @Argument long projectId) {

        workspaceConnectionFacade.shareConnectionToProject(workspaceId, connectionId, projectId);

        return true;
    }

    @MutationMapping(name = "revokeConnectionFromProject")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean revokeConnectionFromProject(
        @Argument long workspaceId, @Argument long connectionId, @Argument long projectId) {

        workspaceConnectionFacade.revokeConnectionFromProject(workspaceId, connectionId, projectId);

        return true;
    }

    @MutationMapping(name = "promoteConnectionToWorkspace")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean promoteConnectionToWorkspace(@Argument long workspaceId, @Argument long connectionId) {
        workspaceConnectionFacade.promoteToWorkspace(workspaceId, connectionId);

        return true;
    }

    @MutationMapping(name = "promoteAllPrivateConnectionsToWorkspace")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public BulkPromoteResultDTO promoteAllPrivateConnectionsToWorkspace(@Argument long workspaceId) {
        return workspaceConnectionFacade.promoteAllPrivateToWorkspace(workspaceId);
    }

    @MutationMapping(name = "setConnectionProjects")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean setConnectionProjects(
        @Argument long workspaceId, @Argument long connectionId, @Argument List<String> projectIds) {

        // GraphQL ID is a String at the wire level; parse explicitly and translate malformed input
        // into a typed validation exception that Spring GraphQL renders as a client error rather
        // than a 500-ish coercion failure.
        List<Long> parsedIds;

        try {
            parsedIds = projectIds == null
                ? List.of()
                : projectIds.stream()
                    .map(Long::valueOf)
                    .toList();
        } catch (NumberFormatException error) {
            // GraphQlBadRequestException is mapped to ErrorType.BAD_REQUEST by
            // GlobalDataFetcherExceptionResolver in core:graphql:graphql-impl.
            throw new GraphQlBadRequestException(
                "projectIds must be numeric IDs; got '%s'".formatted(projectIds), error);
        }

        workspaceConnectionFacade.setConnectionProjects(workspaceId, connectionId, parsedIds);

        return true;
    }

    // Authorization handled in WorkspaceConnectionFacadeImpl.demoteToPrivate() — admin OR creator,
    // so that workspace connections do not become orphaned if every admin loses their role.
    @MutationMapping(name = "demoteConnectionToPrivate")
    public boolean demoteConnectionToPrivate(@Argument long workspaceId, @Argument long connectionId) {
        workspaceConnectionFacade.demoteToPrivate(workspaceId, connectionId);

        return true;
    }
}
