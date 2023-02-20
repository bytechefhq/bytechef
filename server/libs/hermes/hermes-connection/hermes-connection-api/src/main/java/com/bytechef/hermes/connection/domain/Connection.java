
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

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.tag.domain.Tag;
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

    @Column("connection_version")
    private int connectionVersion;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @MappedCollection(idColumn = "connection_id")
    private Set<ConnectionTag> connectionTags = new HashSet<>();

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String name;

    @Column("parameters")
    private MapWrapper parameters;

    @Transient
    private List<Tag> tags = new ArrayList<>();

    @Version
    private int version;

    public void addTag(Tag tag) {
        if (tag.getId() != null) {
            connectionTags.add(new ConnectionTag(tag));
        }

        tags.add(tag);
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
     * Return the name of a component.
     */
    public String getComponentName() {
        return componentName;
    }

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

    public void setAuthorizationName(String authorizationName) {
        this.authorizationName = authorizationName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setConnectionVersion(int connectionVersion) {
        this.connectionVersion = connectionVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new MapWrapper(parameters);
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

    @Override
    public String toString() {
        return "Connection{" + ", id="
            + id + ", name='"
            + name + '\'' + "authorizationName='"
            + authorizationName + '\'' + ", componentName='"
            + componentName + '\'' + ", connectionTags="
            + connectionTags + ", connectionVersion="
            + connectionVersion + ", parameters="
            + parameters + ", version="
            + version + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }
}
