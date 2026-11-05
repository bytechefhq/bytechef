/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedUserService {

    EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole);

    EmbeddedUser addEmbeddedUser(long userId, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId);

    long countByCustomRoleId(long customRoleId);

    void deleteEmbeddedMemberships(long userId);

    List<EmbeddedUser> getEmbeddedUsers();

    List<EmbeddedUser> getUserEmbeddedUsers(long userId);

    boolean isEmbeddedMember(long userId);

    void removeEnvironmentRole(long userId, Environment environment);

    boolean removeEmbeddedUser(long userId);

    EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole);

    EmbeddedUser setEnvironmentRole(
        long userId, Environment environment, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId);

    EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole);

    EmbeddedUser updateEmbeddedUserRole(long userId, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId);
}
