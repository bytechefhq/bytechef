/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedPageInfoContributor implements InfoContributor {

    private final List<String> allowedParentOrigins;

    EmbeddedPageInfoContributor(ApplicationProperties applicationProperties) {
        this.allowedParentOrigins = EmbeddedAllowedParentOrigins.of(applicationProperties);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("embedded", Map.of("allowedParentOrigins", allowedParentOrigins));
    }
}
