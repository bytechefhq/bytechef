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

package com.bytechef.platform.mcp.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.tenant.domain.TenantKey;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing an MCP server.
 *
 * @author Ivica Cardic
 */
@Table
public final class McpServer {

    public static final String MCP_SERVER_NAME_PREFIX = "__MCP_SERVER__";

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private int type;

    @Column
    private int environment;

    @Column
    private boolean enabled;

    @Column("enforce_tool_authorization")
    private boolean enforceToolAuthorization;

    @Column("authentication_required")
    private boolean authenticationRequired = true;

    @Column("secret_key")
    private String secretKey;

    @Column
    private UUID uuid;

    @MappedCollection(idColumn = "mcp_server_id")
    private Set<McpServerTag> mcpServerTags = new HashSet<>();

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

    public McpServer() {
    }

    public McpServer(String name, PlatformType type, Environment environment) {
        this.enabled = true;
        this.environment = environment.ordinal();
        this.name = name;
        this.secretKey = String.valueOf(TenantKey.of());
        this.type = type.ordinal();
        this.uuid = UUID.randomUUID();
    }

    public McpServer(String name, PlatformType type, Environment environment, boolean enabled) {
        this.enabled = enabled;
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

        McpServer mcpServer = (McpServer) o;

        return Objects.equals(id, mcpServer.id);
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

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnforceToolAuthorization() {
        return enforceToolAuthorization;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public int getVersion() {
        return version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<Long> getTagIds() {
        return mcpServerTags
            .stream()
            .map(McpServerTag::getTagId)
            .toList();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(PlatformType type) {
        this.type = type.ordinal();
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnforceToolAuthorization(boolean enforceToolAuthorization) {
        this.enforceToolAuthorization = enforceToolAuthorization;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setTagIds(List<Long> tagIds) {
        this.mcpServerTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (long tagId : tagIds) {
                mcpServerTags.add(new McpServerTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            setTagIds(List.of());
        } else {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    @Override
    public String toString() {
        return "McpServer{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", environment='" + environment + '\'' +
            ", enabled=" + enabled +
            ", secretKey=" + secretKey +
            ", uuid=" + uuid +
            ", mcpServerTags=" + mcpServerTags +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
