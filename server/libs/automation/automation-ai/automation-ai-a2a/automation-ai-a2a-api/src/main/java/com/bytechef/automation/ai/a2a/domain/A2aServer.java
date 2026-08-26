/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.a2a.domain;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.domain.TenantKey;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing an A2A (Agent2Agent) server: a secret-key-addressed endpoint that exposes one or more
 * agent-backed workflows to external A2A clients as an agent card plus a JSON-RPC {@code message/send} surface.
 *
 * <p>
 * This mirrors {@code McpServer} but is a deliberately independent registration stack: the A2A opt-in surface is
 * modeled by {@code a2a_server}/{@code a2a_project}/{@code a2a_project_workflow}, not by reusing the MCP tables.
 * </p>
 *
 * @author Ivica Cardic
 */
@Table("a2a_server")
public final class A2aServer {

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private int type;

    @Column
    private int environment;

    @Column
    private boolean enabled;

    @Column("authentication_required")
    private boolean authenticationRequired = true;

    @Column("secret_key")
    private String secretKey;

    @Column
    private UUID uuid;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public A2aServer() {
    }

    public A2aServer(String name, String description, PlatformType type, Environment environment) {
        this.description = description;
        this.enabled = true;
        this.environment = environment.ordinal();
        this.name = name;
        this.secretKey = String.valueOf(TenantKey.of());
        this.type = type.ordinal();
        this.uuid = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        A2aServer a2aServer = (A2aServer) o;

        return Objects.equals(id, a2aServer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getVersion() {
        return version;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setType(PlatformType type) {
        this.type = type.ordinal();
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "A2aServer{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", type=" + type +
            ", environment=" + environment +
            ", enabled=" + enabled +
            ", authenticationRequired=" + authenticationRequired +
            ", uuid=" + uuid +
            ", version=" + version +
            '}';
    }
}
