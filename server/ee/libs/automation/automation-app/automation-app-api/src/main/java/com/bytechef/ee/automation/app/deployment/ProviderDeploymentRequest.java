/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProviderDeploymentRequest(
    long connectionId, Map<String, ?> deploymentSettings, @Nullable String gitRef, @Nullable String gitCommitSha,
    Environment environment, @Nullable String versionName) {

    public ProviderDeploymentRequest {
        Validate.notNull(deploymentSettings, "deploymentSettings");
        Validate.notNull(environment, "environment");
    }

    public ProviderDeploymentRequest(
        long connectionId, Map<String, ?> deploymentSettings, @Nullable String gitRef, @Nullable String gitCommitSha,
        Environment environment) {

        this(connectionId, deploymentSettings, gitRef, gitCommitSha, environment, null);
    }
}
