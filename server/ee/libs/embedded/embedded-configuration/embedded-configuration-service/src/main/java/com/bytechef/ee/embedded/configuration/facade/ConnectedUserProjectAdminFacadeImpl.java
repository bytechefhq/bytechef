/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Admin-console-only wrapper over the shared {@link ConnectedUserProjectFacade}. Enforces {@code isTenantAdmin()} at
 * the facade layer so the connected-user-project GraphQL controller carries no controller-layer authorization. The
 * underlying facade stays ungated because it is also reached from the public {@code /v1} API and workflow-execution
 * components.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@PreAuthorize("isTenantAdmin()")
public class ConnectedUserProjectAdminFacadeImpl implements ConnectedUserProjectAdminFacade {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectAdminFacadeImpl(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    @Override
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        connectedUserProjectFacade.deleteProjectWorkflow(connectedUserProjectWorkflowId);
    }

    @Override
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        connectedUserProjectFacade.enableProjectWorkflow(connectedUserProjectWorkflowId, enable);
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, environment);
    }
}
