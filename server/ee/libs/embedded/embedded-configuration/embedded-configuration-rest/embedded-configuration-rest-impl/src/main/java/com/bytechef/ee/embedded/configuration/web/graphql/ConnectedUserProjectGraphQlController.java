/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class ConnectedUserProjectGraphQlController {

    private final ConnectedUserProjectService connectedUserProjectService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectGraphQlController(ConnectedUserProjectService connectedUserProjectService) {
        this.connectedUserProjectService = connectedUserProjectService;
    }

    @QueryMapping
    public ConnectedUserProject connectedUserProject(@Argument long id) {
        return connectedUserProjectService.getConnectedUserProject(id);
    }
}
