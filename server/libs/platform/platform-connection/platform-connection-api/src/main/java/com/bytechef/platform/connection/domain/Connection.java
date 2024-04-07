/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Column("authorization_name")
    private String authorizationName;

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
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String name;

    @Column("parameters")
    private EncryptedMapWrapper parameters;

    @Column
    private int type;

    @Version
    private int version;

    public Connection() {
        this.parameters = new EncryptedMapWrapper(Collections.emptyMap());
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
     * Return the name of an authorization it is used with this connection.
     */
    public String getAuthorizationName() {
        return authorizationName;
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
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public CredentialStatus getCredentialStatus() {
        return CredentialStatus.values()[credentialStatus];
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
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
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getLastModifiedDate() {
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

    public List<Long> getTagIds() {
        return connectionTags
            .stream()
            .map(ConnectionTag::getTagId)
            .toList();
    }

    public Type getType() {
        return Type.values()[type];
    }

    public int getVersion() {
        return version;
    }

    public void putAllParameters(Map<String, ?> parameters) {
        this.parameters.putAll(parameters);
    }

    public void setAuthorizationName(String authorizationName) {
        this.authorizationName = authorizationName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setConnectionVersion(int connectionVersion) {
        this.connectionVersion = connectionVersion;
    }

    public void setCredentialStatus(CredentialStatus credentialStatus) {
        this.credentialStatus = credentialStatus.ordinal();
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

    public void setParameters(Map<String, ?> parameters) {
        if (!MapUtils.isEmpty(parameters)) {
            this.parameters = new EncryptedMapWrapper(parameters);
        }
    }

    public void setType(Type type) {
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
        if (!CollectionUtils.isEmpty(tags)) {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Connection{id="
            + id + ", name='"
            + name + '\'' + "authorizationName='"
            + authorizationName + '\'' + ", componentName='"
            + componentName + ", type='"
            + type + ", connectionVersion='"
            + connectionVersion + ", connectionTags="
            + connectionTags + ", queryParameters="
            + parameters + ", version="
            + version + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private String authorizationName;
        private String componentName;
        private int connectionVersion;
        private Long id;
        private String name;
        private Map<String, Object> parameters;
        private List<Long> tagIds;
        private Type type;
        private int version;

        private Builder() {
        }

        public Builder authorizationName(String authorizationName) {
            this.authorizationName = authorizationName;

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

        public Builder tagIds(List<Long> tagIds) {
            this.tagIds = tagIds;

            return this;
        }

        public Builder type(Type type) {
            this.type = type;

            return this;
        }

        public Builder version(int version) {
            this.version = version;

            return this;
        }

        public Connection build() {
            Connection connection = new Connection();

            connection.setAuthorizationName(authorizationName);
            connection.setComponentName(componentName);
            connection.setConnectionVersion(connectionVersion);
            connection.setId(id);
            connection.setName(name);
            connection.setParameters(parameters);
            connection.setTagIds(tagIds);
            connection.setType(type);
            connection.setVersion(version);

            return connection;
        }
    }
}
