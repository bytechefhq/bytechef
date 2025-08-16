/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.WorkspaceUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("NM")
public interface WorkspaceUserService extends com.bytechef.automation.configuration.service.WorkspaceUserService {

    WorkspaceUser create(long userId, long workspaceId);

    void delete(long id);

    List<WorkspaceUser> getUserWorkspaceUsers(long userId);

    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    void deleteWorkspaceUser(long userId);
}
