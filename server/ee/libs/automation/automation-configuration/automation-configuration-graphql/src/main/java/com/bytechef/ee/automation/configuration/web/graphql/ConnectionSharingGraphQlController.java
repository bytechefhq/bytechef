/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for connection visibility and sharing.
 *
 * <p>
 * Authorization (owner-or-admin) and every validation are enforced on {@link WorkspaceConnectionFacade}, not here, so
 * they protect every caller of the facade rather than this entry point alone. This class only maps arguments.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ConnectionSharingGraphQlController {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionSharingGraphQlController(WorkspaceConnectionFacade workspaceConnectionFacade) {
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @QueryMapping
    public List<Long> connectionGrants(@Argument long workspaceId, @Argument long connectionId) {
        return workspaceConnectionFacade.getConnectionGrants(workspaceId, connectionId);
    }

    @MutationMapping
    public boolean setConnectionVisibility(
        @Argument long workspaceId, @Argument long connectionId, @Argument ResourceVisibility visibility) {

        workspaceConnectionFacade.setConnectionVisibility(workspaceId, connectionId, visibility);

        return true;
    }

    @MutationMapping
    public boolean grantConnectionAccess(
        @Argument long workspaceId, @Argument long connectionId, @Argument long userId) {

        workspaceConnectionFacade.grantConnectionAccess(workspaceId, connectionId, userId);

        return true;
    }

    @MutationMapping
    public boolean revokeConnectionAccess(
        @Argument long workspaceId, @Argument long connectionId, @Argument long userId) {

        workspaceConnectionFacade.revokeConnectionAccess(workspaceId, connectionId, userId);

        return true;
    }
}
