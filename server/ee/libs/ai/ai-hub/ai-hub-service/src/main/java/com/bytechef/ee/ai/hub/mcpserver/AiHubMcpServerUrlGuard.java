/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import com.bytechef.commons.util.UrlValidator;
import com.bytechef.config.ApplicationProperties;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Central SSRF guard for custom MCP server URLs. Wraps the shared {@link UrlValidator} with the configured allowlist
 * ({@code bytechef.ai.hub.mcp-server.allowed-hosts}) so both the registration path
 * ({@link AiHubMcpServerFacadeImpl#addMcpServer}) and the connect path ({@link AiHubMcpClientManager}) reject the same
 * set of internal/loopback/cloud-metadata targets — except hosts an operator has explicitly allowlisted (e.g.
 * {@code localhost} for self-hosted/dev MCP servers).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubMcpServerUrlGuard {

    private final Set<String> allowedHosts;

    public AiHubMcpServerUrlGuard(ApplicationProperties applicationProperties) {
        this.allowedHosts = Set.copyOf(
            applicationProperties.getAi()
                .getHub()
                .getMcpServer()
                .getAllowedHosts());
    }

    /**
     * Throws {@link com.bytechef.commons.util.UrlValidationException} when the URL is not a public http(s) target (or
     * an allowlisted host).
     */
    public void validate(String url) {
        UrlValidator.validate(url, allowedHosts);
    }
}
