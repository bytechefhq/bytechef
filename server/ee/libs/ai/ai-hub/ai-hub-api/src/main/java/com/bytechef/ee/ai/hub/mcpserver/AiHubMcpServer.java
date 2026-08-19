/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A user-registered external MCP (Model Context Protocol) server. Scoped per (user, workspace, environment) like the
 * user-global "added connectors" — every chat the user starts can use its tools (bridging into the agent is a follow-up
 * increment). The agent connects OUT to {@link #url} as an MCP client.
 *
 * <p>
 * {@link #authToken} holds the optional bearer token ENCRYPTED at rest (via the {@code Encryption} service in the
 * facade); it is never returned to the client — the GraphQL surface exposes only whether a token is set.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_mcp_server")
public class AiHubMcpServer {

    @Id
    private Long id;

    @Column("user_id")
    private long userId;

    @Column("workspace_id")
    private long workspaceId;

    @Column
    private int environment;

    @Column
    private String name;

    @Column
    private String url;

    // Optional bearer token, stored ENCRYPTED (Encryption.encrypt). Null when the server needs no auth.
    @Column("auth_token")
    private @Nullable String authToken;

    @Column("enabled")
    private boolean enabled = true;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    public AiHubMcpServer() {
    }

    public AiHubMcpServer(
        long userId, long workspaceId, int environment, String name, String url, @Nullable String authToken) {

        this.userId = userId;
        this.workspaceId = workspaceId;
        this.environment = environment;
        this.name = name;
        this.url = url;
        this.authToken = authToken;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public int getEnvironment() {
        return environment;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public @Nullable String getAuthToken() {
        return authToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAuthToken(@Nullable String authToken) {
        this.authToken = authToken;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AiHubMcpServer{" +
            "id=" + id +
            ", userId=" + userId +
            ", workspaceId=" + workspaceId +
            ", environment=" + environment +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            ", enabled=" + enabled +
            ", version=" + version +
            '}';
    }
}
