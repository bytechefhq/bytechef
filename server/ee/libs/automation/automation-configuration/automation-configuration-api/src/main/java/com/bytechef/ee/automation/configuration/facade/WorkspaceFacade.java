/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceFacade {

    Workspace createWorkspace(Workspace workspace);

    void deleteWorkspace(long id);

    Workspace getWorkspace(long id);

    List<Workspace> getWorkspaces();

    Workspace updateWorkspace(Workspace workspace);
}
