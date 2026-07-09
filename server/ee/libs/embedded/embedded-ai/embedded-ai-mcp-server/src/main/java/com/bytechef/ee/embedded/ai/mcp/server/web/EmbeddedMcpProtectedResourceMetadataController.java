/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Serves the RFC 9728 protected-resource metadata for an embedded MCP endpoint at
 * {@code /.well-known/oauth-protected-resource/api/embedded/{secretKey}/mcp}. The metadata advertises the tenant's
 * embedded-MCP identity provider as the authorization server, so a generic MCP client can discover it and authenticate
 * directly against it. Returns 404 when the tenant has no identity provider flagged for embedded MCP.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class EmbeddedMcpProtectedResourceMetadataController {

    private final EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpProtectedResourceMetadataController(
        EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver) {

        this.embeddedMcpProtectedResourceMetadataResolver = embeddedMcpProtectedResourceMetadataResolver;
    }

    @GetMapping("/.well-known/oauth-protected-resource/api/embedded/{secretKey}/mcp")
    public ResponseEntity<ProtectedResourceMetadata> getProtectedResourceMetadata(@PathVariable String secretKey) {
        String resource = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/embedded/" + secretKey + "/mcp")
            .toUriString();

        return embeddedMcpProtectedResourceMetadataResolver.resolveMetadata(resource)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
