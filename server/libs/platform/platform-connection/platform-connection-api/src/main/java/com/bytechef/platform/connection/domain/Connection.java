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

package com.bytechef.platform.connection.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
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
 * @author Ivica Cardic
 */
@Table
public final class Connection {

    public enum CredentialStatus {
        INVALID, VALID
    }

    @Column("authorization_type")
    private Integer authorizationType;

    @Column
    private int environment;

    @Column("component_name")
    private String componentName;

    @Column("connection_version")
    private int connectionVersion = 1;

    @MappedCollection(idColumn = "connection_id")
    private Set<ConnectionTag> connectionTags = new HashSet<>();

    @Column("credential_status")
    private int credentialStatus = 1;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("parameters")
    private EncryptedMapWrapper parameters;

    @Column
    private int status;

    @Column
    private int type;

    @Version
    private int version;

    @Column
    private int visibility;

    public Connection() {
        this.parameters = new EncryptedMapWrapper(Collections.emptyMap());
        this.status = ConnectionStatus.ACTIVE.ordinal();
        this.visibility = ConnectionVisibility.PRIVATE.ordinal();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Connection connection = (Connection) o;

        return id.equals(connection.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * Return the type of an authorization it is used with this connection.
     */
    @Nullable
    public AuthorizationType getAuthorizationType() {
        return authorizationType == null ? null : AuthorizationType.values()[authorizationType];
    }

    /**
     * Return the name of a component this connection can be used for.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Return the version of a component this connection can be used for.
     */
    public int getConnectionVersion() {
        return connectionVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Return the time when the connection was originally created.
     *
     * @return {@link Instant}
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    public CredentialStatus getCredentialStatus() {
        return CredentialStatus.values()[credentialStatus];
    }

    public int getEnvironmentId() {
        return environment;
    }

    /**
     * Return the ID of the connection.
     */
    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Return the time when the connection was updated.
     *
     * @return {@link Instant}
     */
    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Return the connection name.
     */
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {
        return (T) MapUtils.get(parameters.getMap(), name);
    }

    /**
     * Return the connection parameters.
     */
    public Map<String, ?> getParameters() {
        return Collections.unmodifiableMap(parameters == null ? Map.of() : parameters.getMap());
    }

    public ConnectionStatus getStatus() {
        ConnectionStatus[] values = ConnectionStatus.values();

        if (status < 0 || status >= values.length) {
            throw new IllegalStateException(
                "Connection id=%s has invalid status ordinal %d (valid range: 0-%d)".formatted(
                    id, status, values.length - 1));
        }

        return values[status];
    }

    public List<Long> getTagIds() {
        return connectionTags.stream()
            .map(ConnectionTag::getTagId)
            .toList();
    }

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    public int getVersion() {
        return version;
    }

    public ConnectionVisibility getVisibility() {
        ConnectionVisibility[] values = ConnectionVisibility.values();

        if (visibility < 0 || visibility >= values.length) {
            throw new IllegalStateException(
                "Connection id=%s has invalid visibility ordinal %d (valid range: 0-%d)".formatted(
                    id, visibility, values.length - 1));
        }

        return values[visibility];
    }

    public void putAllParameters(Map<String, ?> parameters) {
        this.parameters.putAll(parameters);
    }

    public void setAuthorizationType(AuthorizationType authorizationType) {
        this.authorizationType = authorizationType == null ? null : authorizationType.ordinal();
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Overrides the auditing-populated creator login. Only the reassignment flow should call this directly; Spring Data
     * auditing populates {@code createdBy} automatically for normal creates. Callers must also transition the
     * connection's status (e.g. via {@code ConnectionService#updateConnectionStatus}) so that provenance and status
     * stay in sync — otherwise a successful reassignment leaves {@code PENDING_REASSIGNMENT} in place and an operator
     * cannot tell the flow completed.
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setConnectionVersion(int connectionVersion) {
        this.connectionVersion = connectionVersion;
    }

    public void setCredentialStatus(CredentialStatus credentialStatus) {
        Objects.requireNonNull(credentialStatus, "credentialStatus");

        this.credentialStatus = credentialStatus.ordinal();
    }

    public void setStatus(ConnectionStatus status) {
        Objects.requireNonNull(status, "status");

        ConnectionStatus currentStatus = getStatus();

        if (currentStatus != status && !currentStatus.canTransitionTo(status)) {
            throw new IllegalStateException(
                "Cannot transition connection status from %s to %s".formatted(currentStatus, status));
        }

        this.status = status.ordinal();
    }

    public void setEnvironmentId(int environmentId) {
        this.environment = environmentId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(Map<String, ?> parameters) {
        if (!MapUtils.isEmpty(parameters)) {
            this.parameters = new EncryptedMapWrapper(parameters);
        }
    }

    public void setType(PlatformType type) {
        this.type = type.ordinal();
    }

    public void setTagIds(List<Long> tagIds) {
        this.connectionTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                connectionTags.add(new ConnectionTag(tagId));
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

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Assign visibility. Rejects illegal transitions via {@link ConnectionVisibility#canTransitionTo} — ORGANIZATION is
     * not reachable from workspace-scoped transitions, PRIVATE cannot jump straight to ORGANIZATION, etc. Same-state
     * writes are a silent no-op so idempotent callers (e.g. {@code shareConnectionToProject} re-setting visibility to
     * PROJECT) are unaffected. Related invariants such as removing project shares when demoting to PRIVATE remain the
     * caller's responsibility (typically {@code WorkspaceConnectionFacade}).
     *
     * <p>
     * The state machine applies only to persisted connections (i.e. {@code id != null}). While a connection is still
     * being assembled pre-persist (e.g. {@code ConnectionFacadeImpl.create} downgrading an incoming
     * {@code ORGANIZATION} request to PRIVATE for defense-in-depth), setVisibility is a plain field assignment — there
     * is no "transition" until a row exists in the database.
     */
    public void setVisibility(ConnectionVisibility visibility) {
        Objects.requireNonNull(visibility, "visibility");

        ConnectionVisibility currentVisibility = getVisibility();

        if (currentVisibility == visibility) {
            return;
        }

        if (id != null && !currentVisibility.canTransitionTo(visibility)) {
            throw new IllegalStateException(
                "Cannot transition connection visibility from %s to %s".formatted(currentVisibility, visibility));
        }

        this.visibility = visibility.ordinal();
    }

    @Override
    public String toString() {
        return "Connection{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", componentName='" + componentName + '\'' +
            ", authorizationType='" + authorizationType + '\'' +
            ", connectionVersion=" + connectionVersion +
            ", environment=" + environment +
            ", credentialStatus=" + credentialStatus +
            ", connectionTags=" + connectionTags +
            ", status=" + status +
            ", type=" + type +
            ", visibility=" + visibility +
            ", parameters=" + parameters +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private AuthorizationType authorizationType;
        private String componentName;
        private int connectionVersion;
        private Long id;
        private String name;
        private Map<String, Object> parameters;
        private ConnectionStatus status;
        private List<Long> tagIds;
        private PlatformType type;
        private int version;
        private ConnectionVisibility visibility;

        private Builder() {
        }

        public Builder authorizationType(AuthorizationType authorizationType) {
            this.authorizationType = authorizationType;

            return this;
        }

        public Builder componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        public Builder connectionVersion(int connectionVersion) {
            this.connectionVersion = connectionVersion;

            return this;
        }

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder name(String name) {
            this.name = name;

            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;

            return this;
        }

        public Builder status(ConnectionStatus status) {
            this.status = status;

            return this;
        }

        public Builder tagIds(List<Long> tagIds) {
            this.tagIds = tagIds;

            return this;
        }

        public Builder type(PlatformType type) {
            this.type = type;

            return this;
        }

        public Builder version(int version) {
            this.version = version;

            return this;
        }

        public Builder visibility(ConnectionVisibility visibility) {
            this.visibility = visibility;

            return this;
        }

        public Connection build() {
            Connection connection = new Connection();

            connection.setAuthorizationType(authorizationType);
            connection.setComponentName(componentName);
            connection.setConnectionVersion(connectionVersion);
            connection.setId(id);
            connection.setName(name);
            connection.setParameters(parameters);
            connection.setTagIds(tagIds);
            connection.setType(type);
            connection.setVersion(version);

            if (status != null) {
                connection.setStatus(status);
            }

            if (visibility != null) {
                connection.setVisibility(visibility);
            }

            return connection;
        }
    }
}
