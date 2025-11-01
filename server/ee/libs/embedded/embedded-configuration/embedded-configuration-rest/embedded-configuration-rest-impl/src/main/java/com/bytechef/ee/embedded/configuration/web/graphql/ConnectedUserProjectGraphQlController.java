/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserProjectGraphQlController {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectGraphQlController(
        ConnectedUserProjectFacade connectedUserProjectFacade, EnvironmentService environmentService) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<ConnectedUserProjectDTO> connectedUserProjects(
        @Argument Long connectedUserId, @Argument Long environmentId) {

        return connectedUserProjectFacade.getConnectedUserProjects(
            connectedUserId, environmentService.getEnvironment(environmentId));
    }
}
