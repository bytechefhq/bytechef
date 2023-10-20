
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.connection.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.tag.domain.Tag;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Table
public final class Connection implements Persistable<Long> {

    @Column("authorization_name")
    private String authorizationName;

    @Column("component_name")
    private String componentName;

    @MappedCollection(idColumn = "connection_id")
    private Set<ConnectionTag> connectionTags = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @Column
    private String key;

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

    @Transient
    private List<Tag> tags = new ArrayList<>();

    @Version
    private int version;

    public Connection() {
        this.parameters = new EncryptedMapWrapper(Collections.emptyMap());
    }

    public void addTag(Tag tag) {
        if (tag.getId() != null) {
            connectionTags.add(new ConnectionTag(tag));
        }

        tags.add(tag);
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

    /**
     * Return the ID of the connection.
     */
    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
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

    public <T> T getParameter(String name) {
        return MapValueUtils.get(parameters.getMap(), name, new ParameterizedTypeReference<>() {});
    }

    /**
     * Return the connection parameters.
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters.getMap());
    }

    public List<Long> getTagIds() {
        return connectionTags
            .stream()
            .map(ConnectionTag::getTagId)
            .toList();
    }

    public List<Tag> getTags() {
        return List.copyOf(tags);
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void putAllParameters(Map<String, Object> parameters) {
        this.parameters.putAll(parameters);
    }

    public void setAuthorizationName(String authorizationName) {
        this.authorizationName = authorizationName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new EncryptedMapWrapper(parameters);
    }

    public void setTags(List<Tag> tags) {
        this.connectionTags = new HashSet<>();
        this.tags = new ArrayList<>();

        if (!CollectionUtils.isEmpty(tags)) {
            for (Tag tag : tags) {
                addTag(tag);
            }
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Context.Connection toContextConnection() {
        return new ConnectionImpl(this);
    }

    @Override
    public String toString() {
        return "Connection{" + ", id="
            + id + ", name='"
            + name + '\'' + ", key='"
            + key + '\'' + "authorizationName='"
            + authorizationName + '\'' + ", componentName='"
            + componentName + '\'' + ", connectionTags="
            + connectionTags + ", parameters="
            + parameters + ", version="
            + version + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }

    public Connection update(Connection connection) {
        this.setName(connection.getName());
        this.setVersion(connection.getVersion());

        return this;
    }

    private static class ConnectionImpl implements Context.Connection {
        private final Map<String, Object> parameters;

        public ConnectionImpl(Connection connection) {
            this.parameters = connection.getParameters();
        }

        @Override
        public boolean containsParameter(String name) {
            return parameters.containsKey(name);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getParameter(String name) {
            return (T) parameters.get(name);
        }

        @Override
        public <T> T getParameter(String name, T defaultValue) {
            return MapValueUtils.get(parameters, name, new ParameterizedTypeReference<>() {}, defaultValue);
        }

        @Override
        public String toString() {
            return "ConnectionImpl{" +
                ", parameters=" + parameters +
                '}';
        }
    }
}
