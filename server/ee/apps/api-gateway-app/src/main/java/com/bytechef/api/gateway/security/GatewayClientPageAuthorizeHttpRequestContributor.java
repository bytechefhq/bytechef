/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway.security;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Opens the client's standalone pages, which the gateway serves as the only host of the client. In the monolith the
 * modules owning each page open it; those modules do not run in the gateway.
 *
 * @version ee
 */
@Component
class GatewayClientPageAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    private static final List<String> EMBEDDED_PAGE_PATHS = List.of(
        "/automation-hub.html", "/integration-marketplace.html", "/workflow-builder.html");
    private static final List<String> PUBLIC_PAGE_PATHS = List.of("/connect.html", "/oauth.html");

    private final List<String> allowedParentOrigins;

    GatewayClientPageAuthorizeHttpRequestContributor(ApplicationProperties applicationProperties) {
        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        List<String> configuredAllowedParentOrigins = embedded.getAllowedParentOrigins();

        this.allowedParentOrigins = configuredAllowedParentOrigins == null
            ? List.of()
            : configuredAllowedParentOrigins.stream()
                .map(StringUtils::trim)
                .filter(StringUtils::isNotEmpty)
                .map(origin -> StringUtils.removeEnd(origin, "/"))
                .distinct()
                .toList();
    }

    @Override
    public List<String> getFrameAncestors() {
        return allowedParentOrigins;
    }

    @Override
    public List<String> getFrameablePermitAllRequestMatcherPaths() {
        return EMBEDDED_PAGE_PATHS;
    }

    @Override
    public List<String> getPermitAllRequestMatcherPaths() {
        return PUBLIC_PAGE_PATHS;
    }
}
