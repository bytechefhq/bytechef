/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Permits the MCP connection setup page so it can be accessed without session authentication. The page decodes a
 * short-lived JWT token from the URL to get the connection parameters.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedMcpServerAuthorizeHttpRequestContributor implements AuthorizeHttpRequestContributor {

    @Override
    public List<String> getPermitAllRequestMatcherPaths() {
        return List.of("/connect.html");
    }
}
