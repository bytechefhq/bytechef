/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectAdminFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The embedded admin console's connected-user-projects surface — an admin acts across connected users, so its methods
 * take a row id and derive the owning user from the row. Authorization is enforced at the facade layer:
 * {@link ConnectedUserProjectAdminFacade} wraps the shared, {@code @SkipAutomationAuthorization}-carrying facade behind
 * {@code isTenantAdmin()}, so this controller needs no controller-layer gate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserProjectGraphQlController {

    private final ConnectedUserProjectAdminFacade connectedUserProjectFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectGraphQlController(
        ConnectedUserProjectAdminFacade connectedUserProjectFacade, EnvironmentService environmentService) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<ConnectedUserProjectDTO> connectedUserProjects(
        @Argument Long connectedUserId, @Argument Long environmentId) {

        return connectedUserProjectFacade.getConnectedUserProjects(
            connectedUserId, environmentService.getEnvironment(environmentId));
    }

    @MutationMapping
    public boolean deleteConnectedUserProjectWorkflow(@Argument long id) {
        connectedUserProjectFacade.deleteProjectWorkflow(id);

        return true;
    }

    @MutationMapping
    public boolean enableConnectedUserProjectWorkflow(@Argument long id, @Argument boolean enable) {
        connectedUserProjectFacade.enableProjectWorkflow(id, enable);

        return true;
    }
}
