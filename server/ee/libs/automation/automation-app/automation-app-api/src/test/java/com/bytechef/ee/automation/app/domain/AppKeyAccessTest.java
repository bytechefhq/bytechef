/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppKeyAccessTest {

    @Test
    void testActiveInstanceOfDraftAppIsAllowed() {
        assertThat(AppKeyAccess.isAllowed(AppStatus.DRAFT, AppInstanceStatus.ACTIVE)).isTrue();
    }

    @Test
    void testActiveInstanceOfActiveAppIsAllowed() {
        assertThat(AppKeyAccess.isAllowed(AppStatus.ACTIVE, AppInstanceStatus.ACTIVE)).isTrue();
    }

    @Test
    void testSuspendedAppIsRejectedForEveryInstanceStatus() {
        for (AppInstanceStatus instanceStatus : AppInstanceStatus.values()) {
            assertThat(AppKeyAccess.isAllowed(AppStatus.SUSPENDED, instanceStatus)).isFalse();
        }
    }

    @Test
    void testArchivedAppIsRejectedForEveryInstanceStatus() {
        for (AppInstanceStatus instanceStatus : AppInstanceStatus.values()) {
            assertThat(AppKeyAccess.isAllowed(AppStatus.ARCHIVED, instanceStatus)).isFalse();
        }
    }

    @Test
    void testSuspendedInstanceIsRejectedForEveryAppStatus() {
        for (AppStatus appStatus : AppStatus.values()) {
            assertThat(AppKeyAccess.isAllowed(appStatus, AppInstanceStatus.SUSPENDED)).isFalse();
        }
    }

    @Test
    void testTerminatedInstanceIsRejectedForEveryAppStatus() {
        for (AppStatus appStatus : AppStatus.values()) {
            assertThat(AppKeyAccess.isAllowed(appStatus, AppInstanceStatus.TERMINATED)).isFalse();
        }
    }
}
