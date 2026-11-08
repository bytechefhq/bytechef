/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(name = "bytechef.oauth2.resource-server.issuers[0].uri")
public class McpTenantProtectedResourceMetadataController {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    private final McpTenantProtectedResourceMetadataResolver mcpTenantProtectedResourceMetadataResolver;
    private final @Nullable String publicUrl;

    @SuppressFBWarnings("EI2")
    public McpTenantProtectedResourceMetadataController(
        McpTenantProtectedResourceMetadataResolver mcpTenantProtectedResourceMetadataResolver,
        @Value("${bytechef.public-url:}") @Nullable String publicUrl) {

        this.mcpTenantProtectedResourceMetadataResolver = mcpTenantProtectedResourceMetadataResolver;
        this.publicUrl = publicUrl;
    }

    @GetMapping("/.well-known/oauth-protected-resource/api/{surface}/{secretKey}/mcp")
    public ResponseEntity<McpProtectedResourceMetadata> getProtectedResourceMetadata(HttpServletRequest request) {
        String path = McpProtectedResourceUrls.getPathWithinApplication(request);

        String resource = McpProtectedResourceUrls.resolveResourceUrl(
            publicUrl, McpProtectedResourceUrls.getRequestBaseUrl(request), path.substring(WELL_KNOWN_PREFIX.length()));

        return mcpTenantProtectedResourceMetadataResolver.resolveMetadata(resource)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
