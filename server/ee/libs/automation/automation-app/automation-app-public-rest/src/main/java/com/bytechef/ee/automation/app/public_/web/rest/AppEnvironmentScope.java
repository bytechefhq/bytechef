/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.jspecify.annotations.Nullable;

/**
 * Scopes the management API's by-id app routes to the request's verified environment, answering an app of another
 * environment exactly like a missing one before any permission check runs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AppEnvironmentScope {

    private AppEnvironmentScope() {
    }

    /**
     * Reads the app through the facade's guarded {@link AppFacade#getApp(long)} after the environment check.
     *
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_NOT_FOUND} when the app is missing or
     *                                                       belongs to another environment
     */
    static AppDTO getApp(
        AppFacade appFacade, EnvironmentService environmentService, long id,
        @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(appFacade, environmentService, id, xEnvironment);

        return appFacade.getApp(id);
    }

    /**
     * Requires the app to exist in the request's environment without reading it.
     *
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_NOT_FOUND} when the app is missing or
     *                                                       belongs to another environment
     */
    static void requireInEnvironment(
        AppFacade appFacade, EnvironmentService environmentService, long id,
        @Nullable EnvironmentModel xEnvironment) {

        Environment environment = environmentService.getEnvironment(
            xEnvironment == null ? null : xEnvironment.name());

        appFacade.requireAppInEnvironment(id, environment);
    }
}
