/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppContextDTO(
    long appId, String appName, long appInstanceId, String appInstanceName, Environment environment,
    Map<String, ?> configuration, AppUserAuthMode userAuthMode, @Nullable String issuer, @Nullable String clientId,
    @Nullable String authorizationEndpoint, @Nullable String tokenEndpoint) {
}
