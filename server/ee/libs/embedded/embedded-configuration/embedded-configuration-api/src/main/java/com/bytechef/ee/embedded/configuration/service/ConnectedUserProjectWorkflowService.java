/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectWorkflowService {

    ConnectedUserProjectWorkflow create(ConnectedUserProjectWorkflow connectedUserProjectWorkflow);

    void delete(long id);

    ConnectedUserProjectWorkflow getConnectedUserProjectWorkflow(long id);

    List<ConnectedUserProjectWorkflow> getConnectedUserProjectWorkflows();

    List<ConnectedUserProjectWorkflow> getConnectedUserProjectWorkflows(long connectedUserProjectId);

    ConnectedUserProjectWorkflow getConnectedUserProjectWorkflows(
        long connectedUserProjectId, long projectWorkflowId);

    boolean isConnectionUsed(long connectionId);
}
