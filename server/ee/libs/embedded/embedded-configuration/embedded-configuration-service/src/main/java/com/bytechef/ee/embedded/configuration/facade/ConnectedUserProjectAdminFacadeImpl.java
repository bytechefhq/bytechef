/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectedUserProjectAdminFacadeImpl implements ConnectedUserProjectAdminFacade {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectAdminFacadeImpl(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserProjectWorkflowId, 'ConnectedUserProjectWorkflow', " +
        "'CONNECTED_USER_DELETE')")
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        AutomationAuthorizationContext
            .runSkippingChecks(() -> connectedUserProjectFacade.deleteProjectWorkflow(connectedUserProjectWorkflowId));
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserProjectWorkflowId, 'ConnectedUserProjectWorkflow', " +
        "'CONNECTED_USER_MANAGE')")
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        AutomationAuthorizationContext.runSkippingChecks(
            () -> connectedUserProjectFacade.enableProjectWorkflow(connectedUserProjectWorkflowId, enable));
    }

    @Override
    @PreAuthorize("#connectedUserId == null ? hasEmbeddedScopeInEnvironment('CONNECTED_USER_VIEW', #environment) : " +
        "hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_VIEW')")
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, environment);
    }
}
