/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment of an AppInstance from its row, so the environment-aware gate applies the caller's role in
 * that environment. An app spans environments, so the "App" type has no environment resolver.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppInstanceEnvironmentResolver implements ResourceEnvironmentResolver {

    private final AppInstanceService appInstanceService;

    @SuppressFBWarnings("EI")
    public AppInstanceEnvironmentResolver(AppInstanceService appInstanceService) {
        this.appInstanceService = appInstanceService;
    }

    @Override
    public String resourceType() {
        return "AppInstance";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return appInstanceService.fetchAppInstance(number.longValue())
            .map(AppInstance::getEnvironment);
    }
}
