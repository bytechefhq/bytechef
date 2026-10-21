/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ProviderDeployment(
    String id, AppDeploymentStatus status, @Nullable String url, @Nullable String errorMessage) {

    public ProviderDeployment {
        Validate.notBlank(id, "id");
        Validate.notNull(status, "status");
    }

    public ProviderDeployment(String id, AppDeploymentStatus status, @Nullable String url) {
        this(id, status, url, null);
    }
}
