/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder.util;

import com.bytechef.platform.configuration.domain.Environment;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmbeddedWorkflowBuilderUtils {

    private EmbeddedWorkflowBuilderUtils() {
    }

    public static Environment resolveEnvironment(@Nullable String environment) {
        if (StringUtils.isBlank(environment)) {
            return Environment.PRODUCTION;
        }

        return Environment.valueOf(StringUtils.upperCase(environment));
    }
}
