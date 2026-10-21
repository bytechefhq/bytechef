/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProviderDeploymentRef(long connectionId, Map<String, ?> deploymentSettings, String providerDeploymentId) {

    public ProviderDeploymentRef {
        Validate.notNull(deploymentSettings, "deploymentSettings");
        Validate.notBlank(providerDeploymentId, "providerDeploymentId");
    }
}
