/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Authentication for an OAuth2 Bearer JWT presented to the embedded MCP endpoint. Unauthenticated form carries the
 * external user id (token subject), tenant, environment, and the authorities mapped from the token's group claim (used
 * for per-component tool authorization); the authenticated form carries the resolved connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerOAuth2AuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String externalUserId;
    private List<GrantedAuthority> mappedAuthorities = List.of();
    private String mcpServerSecretKey;

    public EmbeddedMcpServerOAuth2AuthenticationToken(
        long environmentId, String externalUserId, String tenantId, String mcpServerSecretKey,
        List<GrantedAuthority> mappedAuthorities) {

        super(environmentId, tenantId);

        this.externalUserId = externalUserId;
        this.mappedAuthorities = List.copyOf(mappedAuthorities);
        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerOAuth2AuthenticationToken(User user) {
        super(user);
    }

    /**
     * The authenticated form. Carries the environment forward: this token lands in the {@code SecurityContext}, and
     * ticket 1051's {@code ConnectedUserResourceMembershipResolver} decides whether a principal is a connected user --
     * and in which environment -- from exactly that. Built with the {@code User}-only constructor above it would report
     * ordinal 0 (DEVELOPMENT) for every caller.
     */
    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerOAuth2AuthenticationToken(long environmentId, User user) {
        super(environmentId, user);
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    @SuppressFBWarnings("EI")
    public List<GrantedAuthority> getMappedAuthorities() {
        return mappedAuthorities;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
