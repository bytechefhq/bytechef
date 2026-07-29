/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceAdminFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * The embedded admin console's read path over automation-bridge references, keyed by catalog workflow uuid.
 * Authorization is enforced at the facade layer: {@link ConnectedUserCodeWorkflowReferenceAdminFacade} wraps the lookup
 * behind {@code isTenantAdmin()}, so this controller needs no controller-layer gate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserCodeWorkflowReferenceGraphQlController {

    private final ConnectedUserCodeWorkflowReferenceAdminFacade connectedUserCodeWorkflowReferenceAdminFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserCodeWorkflowReferenceGraphQlController(
        ConnectedUserCodeWorkflowReferenceAdminFacade connectedUserCodeWorkflowReferenceAdminFacade) {

        this.connectedUserCodeWorkflowReferenceAdminFacade = connectedUserCodeWorkflowReferenceAdminFacade;
    }

    @QueryMapping
    public List<ConnectedUserCodeWorkflowReferenceDTO> connectedUserCodeWorkflowReferences(
        @Argument List<String> catalogWorkflowUuids) {

        return connectedUserCodeWorkflowReferenceAdminFacade.getReferences(Set.copyOf(catalogWorkflowUuids));
    }
}
