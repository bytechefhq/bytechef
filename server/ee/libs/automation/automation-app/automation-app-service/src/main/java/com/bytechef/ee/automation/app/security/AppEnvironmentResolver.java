/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the environment an App lives in from its row, so the environment-aware gate can apply the caller's role in
 * that environment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppEnvironmentResolver implements ResourceEnvironmentResolver {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppEnvironmentResolver(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return appService.fetchApp(number.longValue())
            .map(App::getEnvironment);
    }
}
