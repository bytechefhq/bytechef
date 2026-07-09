/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the RFC 9728 protected-resource metadata for an automation or management MCP endpoint at
 * {@code /.well-known/oauth-protected-resource/api/{surface}/{secretKey}/mcp}. The metadata advertises the tenant's
 * external identity providers (and ByteChef's own issuers) as authorization servers, so a generic MCP client can
 * discover them and authenticate directly. Returns 404 when the tenant has no authorization server for the surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(name = "bytechef.oauth2.resource-server.issuers[0].uri")
public class McpTenantProtectedResourceMetadataController {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    private final McpTenantProtectedResourceMetadataResolver mcpTenantProtectedResourceMetadataResolver;

    @SuppressFBWarnings("EI2")
    public McpTenantProtectedResourceMetadataController(
        McpTenantProtectedResourceMetadataResolver mcpTenantProtectedResourceMetadataResolver) {

        this.mcpTenantProtectedResourceMetadataResolver = mcpTenantProtectedResourceMetadataResolver;
    }

    @GetMapping("/.well-known/oauth-protected-resource/api/{surface}/{secretKey}/mcp")
    public ResponseEntity<McpProtectedResourceMetadata> getProtectedResourceMetadata(HttpServletRequest request) {
        String requestUrl = UrlUtils.buildFullRequestUrl(request);

        int queryIndex = requestUrl.indexOf('?');

        if (queryIndex >= 0) {
            requestUrl = requestUrl.substring(0, queryIndex);
        }

        // The resource is this metadata URL with the well-known prefix removed, i.e. the MCP endpoint itself.
        String resource = requestUrl.replace(WELL_KNOWN_PREFIX, "");

        return mcpTenantProtectedResourceMetadataResolver.resolveMetadata(resource)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
