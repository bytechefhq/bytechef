/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecretVerifier implements McpServerSecretVerifier {

    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("/api/embedded/(.+)/mcp");

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpServerSecretVerifier(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Optional<Boolean> verifyMcpServerSecret(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return Optional.empty();
        }

        String mcpServerSecretKey = matcher.group(1);

        McpServer mcpServer;

        try {
            mcpServer = mcpServerService.getMcpServer(mcpServerSecretKey);
        } catch (RuntimeException runtimeException) {
            return Optional.of(false);
        }

        String secretKey = mcpServer.getSecretKey();

        return Optional.of(
            mcpServer.isEnabled() && mcpServer.getType() == PlatformType.EMBEDDED && secretKey != null &&
                MessageDigest.isEqual(
                    secretKey.getBytes(StandardCharsets.UTF_8), mcpServerSecretKey.getBytes(StandardCharsets.UTF_8)));
    }
}
