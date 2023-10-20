
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

package com.bytechef.hermes.connection.facade;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.connection.config.OAuth2Properties;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Transactional
public class ConnectionFacadeImpl implements ConnectionFacade {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final OAuth2Properties oAuth2Properties;
    private final TagService tagService;

    @SuppressFBWarnings("EI2")
    public ConnectionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        OAuth2Properties oAuth2Properties, TagService tagService) {

        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.oAuth2Properties = oAuth2Properties;
        this.tagService = tagService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Connection create(Connection connection) {
        if (!CollectionUtils.isEmpty(connection.getTags())) {
            connection.setTags(tagService.save(connection.getTags()));
        }

        if (connection.getAuthorizationName() != null && connection.containsParameter(Authorization.CODE)) {
            Authorization authorization = connectionDefinitionService.getAuthorization(
                connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

            // TODO add support for OAUTH2_AUTHORIZATION_CODE_PKCE

            Authorization.AuthorizationType authorizationType = authorization.getType();

            if (authorizationType == Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                oAuth2Properties.checkPredefinedApp(connection);

                Authorization.AuthorizationCallbackResponse authorizationCallbackResponse = connectionDefinitionService
                    .executeAuthorizationCallback(
                        connection, oAuth2Properties.getRedirectUri());

                connection.putAllParameters(authorizationCallbackResponse.toMap());
            }
        }

        return connectionService.create(connection);
    }

    @Override
    public void delete(Long id) {
//        Connection connection = connectionService.getConnection(id);

        connectionService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        connection.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(Long id) {
        Connection connection = connectionService.getConnection(id);

        connection.setTags(tagService.getTags(connection.getTagIds()));

        return connection;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(List<String> componentNames, List<Long> tagIds) {
        List<Connection> connections = connectionService.getConnections(componentNames, tagIds);

        List<Tag> tags = tagService.getTags(connections.stream()
            .flatMap(connection -> connection.getTagIds()
                .stream())
            .filter(Objects::nonNull)
            .toList());

        for (Connection connection : connections) {
            connection.setTags(
                tags.stream()
                    .filter(tag -> {
                        List<Long> curTagIds = connection.getTagIds();

                        return curTagIds.contains(tag.getId());
                    })
                    .toList());
        }

        return connections;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getConnectionTags() {
        List<Connection> connections = connectionService.getConnections();

        return tagService.getTags(connections.stream()
            .map(Connection::getTagIds)
            .flatMap(Collection::stream)
            .toList());
    }

    @Override
    public Connection update(Long id, List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        return connectionService.update(id, tags);
    }

    @Override
    public Connection update(Connection connection) {
        connection
            .setTags(
                CollectionUtils.isEmpty(connection.getTags())
                    ? Collections.emptyList()
                    : tagService.save(connection.getTags()));

        return connectionService.update(connection);
    }
}
