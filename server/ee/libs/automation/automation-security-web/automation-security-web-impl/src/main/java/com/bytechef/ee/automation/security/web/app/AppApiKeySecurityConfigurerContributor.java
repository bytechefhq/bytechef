/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import com.bytechef.ee.automation.app.security.AppUserAuthenticator;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class AppApiKeySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final AppUserAuthenticator appUserAuthenticator;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public AppApiKeySecurityConfigurerContributor(
        AppInstanceService appInstanceService, AppService appService, AppUserAuthenticator appUserAuthenticator,
        EnvironmentService environmentService) {

        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.appUserAuthenticator = appUserAuthenticator;
        this.environmentService = environmentService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        return (T) new AppApiKeySecurityConfigurer(
            appInstanceService, appService, appUserAuthenticator, environmentService);
    }
}
