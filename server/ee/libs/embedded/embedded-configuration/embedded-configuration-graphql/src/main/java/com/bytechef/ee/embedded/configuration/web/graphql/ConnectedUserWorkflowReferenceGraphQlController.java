/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceAdminFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
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
public class ConnectedUserWorkflowReferenceGraphQlController {
    private final ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceGraphQlController(
        ConnectedUserWorkflowReferenceAdminFacade connectedUserWorkflowReferenceAdminFacade) {
        this.connectedUserWorkflowReferenceAdminFacade = connectedUserWorkflowReferenceAdminFacade;
    }

    @QueryMapping
    public List<ConnectedUserWorkflowReferenceDTO> connectedUserCodeWorkflowReferences(
        @Argument List<String> automationWorkflowUuids) {
        return connectedUserWorkflowReferenceAdminFacade.getReferences(Set.copyOf(automationWorkflowUuids));
    }
}
