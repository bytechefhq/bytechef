/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.jspecify.annotations.Nullable;

/**
 * Scopes the management API to the request's verified environment. A by-id instance route answers an instance of
 * another environment, or one the caller may not view, exactly like a missing one before the guarded facade call runs,
 * and a mutation outside DEVELOPMENT is refused before any facade is called.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AppEnvironmentScope {

    private AppEnvironmentScope() {
    }

    static Environment getEnvironment(
        EnvironmentService environmentService, @Nullable EnvironmentModel xEnvironment) {

        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    /**
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_OPERATION_DEVELOPMENT_ONLY} when the
     *                                                       environment is not DEVELOPMENT
     */
    static void requireDevelopment(Environment environment) {
        if (environment != Environment.DEVELOPMENT) {
            throw new ConfigurationException(
                "App management changes are allowed only in the DEVELOPMENT environment, not in " + environment,
                AppErrorType.APP_OPERATION_DEVELOPMENT_ONLY);
        }
    }

    /**
     * Resolves the request's environment and requires it to be DEVELOPMENT.
     */
    static Environment requireDevelopment(
        EnvironmentService environmentService, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = getEnvironment(environmentService, xEnvironment);

        requireDevelopment(environment);

        return environment;
    }

    /**
     * Requires the instance to exist in the request's environment and to be visible to the caller, through
     * {@link AppInstanceFacade#requireAppInstanceInEnvironment}.
     *
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_INSTANCE_NOT_FOUND} when the instance is
     *                                                       missing, belongs to another environment or is one the
     *                                                       caller may not view
     */
    static void requireInEnvironment(AppInstanceFacade appInstanceFacade, long appInstanceId, Environment environment) {
        appInstanceFacade.requireAppInstanceInEnvironment(appInstanceId, environment);
    }
}
