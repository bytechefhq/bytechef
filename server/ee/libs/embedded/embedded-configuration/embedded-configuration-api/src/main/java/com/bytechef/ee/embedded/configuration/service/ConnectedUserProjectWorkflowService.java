/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectWorkflowService {

    void addConnection(long connectedUserProjectId, long projectWorkflowId, long connectionId);

    ConnectedUserProjectWorkflow create(ConnectedUserProjectWorkflow connectedUserProjectWorkflow);

    void delete(long id);

    Optional<ConnectedUserProjectWorkflow> fetchConnectedUserProjectWorkflow(
        long connectedUserProjectId, long projectWorkflowId);

    ConnectedUserProjectWorkflow getConnectedUserProjectWorkflow(long id);

    ConnectedUserProjectWorkflow getConnectedUserProjectWorkflow(long connectedUserProjectId, long projectWorkflowId);

    List<ConnectedUserProjectWorkflow> getConnectedUserProjectWorkflows(long connectedUserProjectId);

    boolean isConnectionUsed(long connectionId);

    void incrementWorkflowVersion(long connectedUserProjectId, long projectWorkflowId);
}
