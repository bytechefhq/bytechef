/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SuppressFBWarnings("NM")
public interface WorkspaceService extends com.bytechef.automation.configuration.service.WorkspaceService {

    Workspace create(Workspace workspace);

    void delete(long id);

    Workspace getProjectWorkspace(long projectId);

    List<Workspace> getWorkspaces();

    Workspace getWorkspace(long id);

    Workspace update(Workspace workspace);

}
