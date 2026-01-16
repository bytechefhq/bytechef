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

package com.bytechef.platform.connection.dto;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectionDTO(
    boolean active, @Nullable AuthorizationType authorizationType, Map<String, ?> authorizationParameters,
    String baseUri, String componentName, Map<String, ?> connectionParameters, int connectionVersion, String createdBy,
    Instant createdDate, CredentialStatus credentialStatus, int environmentId, Long id, String lastModifiedBy,
    Instant lastModifiedDate, String name, Map<String, ?> parameters, List<Tag> tags, int version) {

    public ConnectionDTO(
        boolean active, Map<String, ?> authorizationParameters, String baseUri, Connection connection,
        Map<String, ?> connectionParameters, List<Tag> tags) {

        this(
            active, connection.getAuthorizationType(), authorizationParameters, baseUri, connection.getComponentName(),
            connectionParameters, connection.getConnectionVersion(), connection.getCreatedBy(),
            connection.getCreatedDate(), connection.getCredentialStatus(), connection.getEnvironmentId(),
            connection.getId(), connection.getLastModifiedBy(), connection.getLastModifiedDate(), connection.getName(),
            connection.getParameters(), tags, connection.getVersion());
    }

    public Connection toConnection() {
        Connection connection = new Connection();

        connection.setAuthorizationType(authorizationType);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setEnvironmentId(environmentId);
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

    public static final class Builder {
        private boolean active;
        private AuthorizationType authorizationType;
        private String baseUri;
        private String componentName;
        private int connectionVersion;
        private String createdBy;
        private Instant createdDate;
        private CredentialStatus credentialStatus;
        private int environmentId;
        private Long id;
        private String lastModifiedBy;
        private Instant lastModifiedDate;
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

        public Builder authorizationType(AuthorizationType authorizationType) {
            this.authorizationType = authorizationType;

            return this;
        }

        public Builder baseUri(String baseUri) {
            this.baseUri = baseUri;

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

        public Builder createdDate(Instant createdDate) {
            this.createdDate = createdDate;

            return this;
        }

        public Builder credentialStatus(CredentialStatus credentialStatus) {
            this.credentialStatus = credentialStatus;

            return this;
        }

        public Builder environmentId(int environmentId) {
            this.environmentId = environmentId;

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

        public Builder lastModifiedDate(Instant lastModifiedDate) {
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
                active, authorizationType, Map.of(), baseUri, componentName, Map.of(), connectionVersion, createdBy,
                createdDate, credentialStatus, environmentId, id, lastModifiedBy, lastModifiedDate, name, parameters,
                tags, version);
        }
    }
}
