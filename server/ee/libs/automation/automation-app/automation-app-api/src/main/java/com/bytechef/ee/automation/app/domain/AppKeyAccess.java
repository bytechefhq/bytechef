/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AppKeyAccess {

    private AppKeyAccess() {
    }

    public static boolean isAllowed(AppStatus appStatus, AppInstanceStatus appInstanceStatus) {
        return (appStatus == AppStatus.DRAFT || appStatus == AppStatus.ACTIVE) &&
            appInstanceStatus == AppInstanceStatus.ACTIVE;
    }
}
