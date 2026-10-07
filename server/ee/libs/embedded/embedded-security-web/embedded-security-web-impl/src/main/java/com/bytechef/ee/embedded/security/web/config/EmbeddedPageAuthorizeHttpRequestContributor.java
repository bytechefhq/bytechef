/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Permits the embedded Automation Hub, Workflow Builder and Integration Marketplace pages and lets customer
 * applications embed them in an iframe. The pages authenticate their API calls with the connected user's JWT token
 * passed in by the embedding application.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedPageAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    private static final List<String> EMBEDDED_PAGE_PATHS = List.of(
        "/automation-hub.html", "/integration-marketplace.html", "/workflow-builder.html");

    @Override
    public List<String> getFrameablePermitAllRequestMatcherPaths() {
        return EMBEDDED_PAGE_PATHS;
    }
}
