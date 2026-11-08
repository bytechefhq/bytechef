/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class McpTenantSecretVerifier {

    private static final Logger log = LoggerFactory.getLogger(McpTenantSecretVerifier.class);

    private final Supplier<List<McpServerSecretVerifier>> mcpServerSecretVerifiersSupplier;

    @SuppressFBWarnings("EI2")
    public McpTenantSecretVerifier(Supplier<List<McpServerSecretVerifier>> mcpServerSecretVerifiersSupplier) {
        this.mcpServerSecretVerifiersSupplier = mcpServerSecretVerifiersSupplier;
    }

    public boolean isVerified(HttpServletRequest request, String tenantId, String mcpPath) {
        HttpServletRequest mcpRequest = mcpPath.equals(request.getServletPath())
            ? request : new McpPathRequest(request, mcpPath);

        List<McpServerSecretVerifier> mcpServerSecretVerifiers = mcpServerSecretVerifiersSupplier.get();

        try {
            return TenantContext.callWithTenantId(
                tenantId,
                () -> mcpServerSecretVerifiers.stream()
                    .map(mcpServerSecretVerifier -> mcpServerSecretVerifier.verifyMcpServerSecret(mcpRequest))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElse(false));
        } catch (RuntimeException runtimeException) {
            log.warn("MCP server secret verification failed in tenant {}", tenantId, runtimeException);

            return false;
        }
    }

    private static class McpPathRequest extends HttpServletRequestWrapper {

        private final String mcpPath;

        McpPathRequest(HttpServletRequest request, String mcpPath) {
            super(request);

            this.mcpPath = mcpPath;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + mcpPath;
        }

        @Override
        public String getServletPath() {
            return mcpPath;
        }
    }
}
