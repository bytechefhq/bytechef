/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web.rest;

import com.bytechef.ee.embedded.ai.mcp.server.service.ConnectTokenService;
import com.bytechef.ee.embedded.ai.mcp.server.service.ConnectTokenService.ConnectTokenData;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for resolving MCP connection setup tokens. The static HTML page at /connect.html calls this endpoint
 * to get the token data needed to render the connection form.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConditionalOnEEVersion
@RequestMapping("/api/embedded/connect")
@RestController
class ConnectController {

    private final ConnectTokenService connectTokenService;

    @SuppressFBWarnings("EI")
    ConnectController(ConnectTokenService connectTokenService) {
        this.connectTokenService = connectTokenService;
    }

    @GetMapping("/{token}")
    ResponseEntity<Map<String, Object>> resolveToken(@PathVariable String token) {
        ConnectTokenData connectTokenData = connectTokenService.resolveToken(token);

        if (connectTokenData == null) {
            return ResponseEntity.notFound()
                .build();
        }

        return ResponseEntity.ok(
            Map.of(
                "componentName", connectTokenData.componentName(),
                "externalUserId", connectTokenData.externalUserId(),
                "mcpServerId", connectTokenData.mcpServerId()));
    }
}
