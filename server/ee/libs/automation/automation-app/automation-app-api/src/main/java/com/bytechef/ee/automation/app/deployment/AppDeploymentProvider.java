/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppDeploymentProvider {

    AppHostingTarget getHostingTarget();

    String getDeploymentConnectionComponentName();

    ProviderDeployment deploy(ProviderDeploymentRequest request);

    ProviderDeployment getDeployment(ProviderDeploymentRef ref);

    void cancel(ProviderDeploymentRef ref);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    default void validateDeploymentSettings(Map<String, ?> deploymentSettings, Environment environment) {
    }
}
