/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedPageAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    private static final List<String> EMBEDDED_PAGE_PATHS = List.of(
        "/automation-hub.html", "/integration-marketplace.html", "/workflow-builder.html");

    private final List<String> allowedParentOrigins;

    EmbeddedPageAuthorizeHttpRequestContributor(ApplicationProperties applicationProperties) {
        this.allowedParentOrigins = EmbeddedAllowedParentOrigins.of(applicationProperties);
    }

    @Override
    public List<String> getFrameAncestors() {
        return List.copyOf(allowedParentOrigins);
    }

    @Override
    public List<String> getFrameablePermitAllRequestMatcherPaths() {
        return EMBEDDED_PAGE_PATHS;
    }
}
