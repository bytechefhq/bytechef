/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * Manages the embedded members of the tenant on behalf of the signed-in user. Authorization is declared on the
 * implementation; the underlying {@code EmbeddedUserService} carries no checks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedUserFacade {

    EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole);

    EmbeddedUser addEmbeddedUserByEmail(String email, WorkspaceRole embeddedRole);

    List<EmbeddedUser> getEmbeddedUsers();

    void removeEnvironmentRole(long userId, Environment environment);

    boolean removeEmbeddedUser(long userId);

    EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole);

    EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole);
}
