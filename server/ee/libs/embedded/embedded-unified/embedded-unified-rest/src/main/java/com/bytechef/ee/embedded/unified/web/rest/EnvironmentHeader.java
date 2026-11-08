/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.web.rest.AbstractEnvironmentHeaderConverter;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EnvironmentHeader {

    private static final AbstractEnvironmentHeaderConverter<Environment> ENVIRONMENT_HEADER_CONVERTER =
        new AbstractEnvironmentHeaderConverter<>(Environment.class) {};

    private EnvironmentHeader() {
    }

    public static Environment toEnvironment(@Nullable String environment) {
        if (environment == null) {
            return Environment.PRODUCTION;
        }

        Environment headerEnvironment = ENVIRONMENT_HEADER_CONVERTER.convert(environment);

        return headerEnvironment == null ? Environment.PRODUCTION : headerEnvironment;
    }
}
