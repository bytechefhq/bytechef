/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.config.ApplicationProperties;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class EmbeddedAllowedParentOrigins {

    private EmbeddedAllowedParentOrigins() {
    }

    static List<String> of(ApplicationProperties applicationProperties) {
        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        List<String> allowedParentOrigins = embedded.getAllowedParentOrigins();

        if (allowedParentOrigins == null) {
            return List.of();
        }

        return allowedParentOrigins.stream()
            .map(StringUtils::trim)
            .filter(StringUtils::isNotEmpty)
            .map(origin -> StringUtils.removeEnd(origin, "/"))
            .distinct()
            .toList();
    }
}
