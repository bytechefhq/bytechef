/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;

/**
 * The status rules the app facades apply before they change what an app instance is bound to.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AppInstanceStatusChecks {

    private AppInstanceStatusChecks() {
    }

    /**
     * Refuses a change to a TERMINATED instance with {@code APP_INSTANCE_TERMINATED}.
     */
    static void checkNotTerminated(AppInstance appInstance) {
        if (appInstance.getStatus() == AppInstanceStatus.TERMINATED) {
            throw new ConfigurationException(
                "App instance %s is terminated".formatted(appInstance.getId()), AppErrorType.APP_INSTANCE_TERMINATED);
        }
    }
}
