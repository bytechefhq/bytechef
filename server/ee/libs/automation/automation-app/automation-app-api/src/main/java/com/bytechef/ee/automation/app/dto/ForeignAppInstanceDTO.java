/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.platform.configuration.domain.Environment;
import org.jspecify.annotations.Nullable;

/**
 * The read-only row an app shows for an instance that lives in another workspace than the app: its name, workspace,
 * environment, status and current version, with no bindings, no key hint and no configuration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ForeignAppInstanceDTO(
    long id, String name, long workspaceId, Environment environment, AppInstanceStatus status,
    @Nullable Long currentAppDeploymentId) {

    public static ForeignAppInstanceDTO of(AppInstance appInstance) {
        return new ForeignAppInstanceDTO(
            appInstance.getId(), appInstance.getName(), appInstance.getWorkspaceId(), appInstance.getEnvironment(),
            appInstance.getStatus(), appInstance.getCurrentAppDeploymentId());
    }
}
