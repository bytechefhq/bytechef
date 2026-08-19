/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserTest {

    @Test
    void testThreeArgFactoryProducesAnImplicitRow() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.EDITOR);

        assertThat(workspaceUser.getEnvironment()).isNull();
    }

    @Test
    void testThreeArgCustomRoleFactoryProducesAnImplicitRow() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(1L, 2L, 9L);

        assertThat(workspaceUser.getEnvironment()).isNull();
    }

    @Test
    void testFourArgFactoryCarriesTheEnvironment() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.VIEWER, Environment.PRODUCTION);

        assertThat(workspaceUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testFourArgFactoryAcceptsANullEnvironment() {
        WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.VIEWER, null);

        assertThat(workspaceUser.getEnvironment()).isNull();
    }

    @Test
    void testCustomRoleFactoryCarriesTheEnvironment() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(1L, 2L, 9L, Environment.DEVELOPMENT);

        assertThat(workspaceUser.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(workspaceUser.getCustomRoleId()).isEqualTo(9L);
    }

    @Test
    void testEveryEnvironmentRoundTripsThroughTheStoredOrdinal() {
        for (Environment environment : Environment.values()) {
            WorkspaceUser workspaceUser = WorkspaceUser.forRole(1L, 2L, WorkspaceRole.EDITOR, environment);

            assertThat(workspaceUser.getEnvironment())
                .as("environment %s must survive the ordinal round trip", environment)
                .isEqualTo(environment);
        }
    }
}
