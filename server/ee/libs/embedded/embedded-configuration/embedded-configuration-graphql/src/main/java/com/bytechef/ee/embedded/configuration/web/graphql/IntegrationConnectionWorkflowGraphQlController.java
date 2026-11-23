/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.IntegrationConnectionWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public class IntegrationConnectionWorkflowGraphQlController {

    private final ConnectionFacade connectionFacade;

    @SuppressFBWarnings("EI")
    public IntegrationConnectionWorkflowGraphQlController(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @QueryMapping(name = "integrationConnectionWorkflows")
    public List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflows(@Argument long connectionId) {
        return connectionFacade.getConnectionWorkflows(connectionId);
    }
}
