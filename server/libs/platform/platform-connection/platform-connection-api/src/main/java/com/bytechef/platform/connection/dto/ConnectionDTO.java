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

package com.bytechef.platform.connection.dto;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectionDTO(
    boolean active, String authorizationName, String componentName, int connectionVersion, String createdBy,
    LocalDateTime createdDate, CredentialStatus credentialStatus, Environment environment, Long id,
    String lastModifiedBy, LocalDateTime lastModifiedDate, String name, Map<String, ?> parameters, List<Tag> tags,
    int version) {

    public ConnectionDTO(boolean active, Connection connection, List<Tag> tags) {
        this(
            active, connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion(),
            connection.getCreatedBy(), connection.getCreatedDate(), connection.getCredentialStatus(),
            connection.getEnvironment(), connection.getId(), connection.getLastModifiedBy(),
            connection.getLastModifiedDate(), connection.getName(), connection.getParameters(), tags,
            connection.getVersion());
    }

    public Connection toConnection() {
        Connection connection = new Connection();

        connection.setAuthorizationName(authorizationName);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setId(id);
        connection.setName(name);
        connection.setParameters(parameters);
        connection.setTags(tags);
        connection.setVersion(version);

        return connection;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private boolean active;
        private String authorizationName;
        private String componentName;
        private int connectionVersion;
        private String createdBy;
        private LocalDateTime createdDate;
        private CredentialStatus credentialStatus;
        private Environment environment;
        private Long id;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private String name;
        private Map<String, Object> parameters;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder active(boolean active) {
            this.active = active;

            return this;
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

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;

            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;

            return this;
        }

        public Builder credentialStatus(CredentialStatus credentialStatus) {
            this.credentialStatus = credentialStatus;

            return this;
        }

        public Builder environment(Environment environment) {
            this.environment = environment;

            return this;
        }

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;

            return this;
        }

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;

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

        public Builder tags(List<Tag> tags) {
            this.tags = tags;

            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public ConnectionDTO build() {
            return new ConnectionDTO(
                active, authorizationName, componentName, connectionVersion, createdBy, createdDate, credentialStatus,
                environment, id, lastModifiedBy, lastModifiedDate, name, parameters, tags, version);
        }
    }
}
