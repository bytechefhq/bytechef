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

package com.bytechef.platform.connection.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.exception.PlatformException;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.registry.domain.BaseProperty;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectionFacadeImpl implements ConnectionFacade {

    private final ConnectionDefinitionFacade connectionDefinitionFacade;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI2")
    public ConnectionFacadeImpl(
        ConnectionDefinitionFacade connectionDefinitionFacade, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService, InstanceAccessorRegistry instanceAccessorRegistry,
        OAuth2Service oAuth2Service, TagService tagService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectionDefinitionFacade = connectionDefinitionFacade;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public ConnectionDTO create(ConnectionDTO connectionDTO, Type type) {
        Connection connection = connectionDTO.toConnection();

        if (StringUtils.isNotBlank(connection.getAuthorizationName()) &&
            connection.containsParameter(Authorization.CODE)) {

            // TODO add support for OAUTH2_AUTHORIZATION_CODE_PKCE

            AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getAuthorizationName());

            if (authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                AuthorizationCallbackResponse authorizationCallbackResponse =
                    connectionDefinitionFacade.executeAuthorizationCallback(
                        connection.getComponentName(),
                        new ComponentConnection(
                            connection.getComponentName(), connection.getConnectionVersion(),
                            oAuth2Service.checkPredefinedParameters(
                                connection.getComponentName(), connection.getParameters()),
                            connection.getAuthorizationName()),
                        oAuth2Service.getRedirectUri());

                connection.putAllParameters(authorizationCallbackResponse.result());
            }
        }

        Map<String, ?> parameters = new HashMap<>(connection.getParameters());

        parameters.remove("state");

        connection.setParameters(parameters);

        List<Tag> tags = checkTags(connectionDTO.tags());

        if (!tags.isEmpty()) {
            connection.setTags(tags);
        }

        connection.setType(type);

        connection = connectionService.create(connection);

        return toConnectionDTO(false, connection, tags);
    }

    @Override
    public void delete(Long id) {
        Connection connection = connectionService.getConnection(id);

        if (isConnectionUsed(id, connection.getType())) {
            throw new PlatformException(
                "Connection id=%s is used".formatted(id), ConnectionErrorType.CONNECTION_IS_USED);
        }

        connectionService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        connection.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionDTO getConnection(Long id) {
        Connection connection = connectionService.getConnection(id);

        return toConnectionDTO(
            isConnectionUsed(Validate.notNull(connection.getId(), "id"), connection.getType()),
            connection, tagService.getTags(connection.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Type type) {

        List<Connection> connections = connectionService.getConnections(componentName, connectionVersion, tagId, type);

        return getConnections(connections);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getConnectionTags(Type type) {
        List<Connection> connections = connectionService.getConnections(type);

        return tagService.getTags(
            connections
                .stream()
                .map(Connection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public ConnectionDTO update(Long id, List<Tag> tags) {
        tags = checkTags(tags);

        Connection connection = connectionService.update(id, CollectionUtils.map(tags, Tag::getId));

        return toConnectionDTO(
            isConnectionUsed(Validate.notNull(connection.getId(), "id"), connection.getType()), connection, tags);
    }

    @Override
    public ConnectionDTO update(ConnectionDTO connectionDTO) {
        List<Tag> tags = checkTags(connectionDTO.tags());

        Connection connection = connectionDTO.toConnection();

        connection = connectionService.update(connection);

        return toConnectionDTO(isConnectionUsed(connectionDTO.id(), connection.getType()), connection, tags);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(Connection connection, Tag tag) {
        List<Long> curTagIds = connection.getTagIds();

        return curTagIds.contains(tag.getId());
    }

    private boolean isConnectionUsed(long connectionId, Type type) {
        boolean connectionUsed;

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(type);

        connectionUsed = instanceAccessor.isConnectionUsed(connectionId);

        if (!connectionUsed) {
            connectionUsed = workflowTestConfigurationService.isConnectionUsed(connectionId);
        }

        return connectionUsed;
    }

    private List<Tag> filterTags(List<Tag> tags, Connection connection) {
        return tags
            .stream()
            .filter(tag -> containsTag(connection, tag))
            .toList();
    }

    private List<ConnectionDTO> getConnections(List<Connection> connections) {
        List<Tag> tags = tagService.getTags(
            connections
                .stream()
                .flatMap(connection -> CollectionUtils.stream(connection.getTagIds()))
                .filter(Objects::nonNull)
                .toList());

        return CollectionUtils.map(
            connections,
            connection -> toConnectionDTO(
                isConnectionUsed(Validate.notNull(connection.getId(), "id"), connection.getType()), connection,
                filterTags(tags, connection)));
    }

    private ConnectionDTO toConnectionDTO(boolean active, Connection connection, List<Tag> tags) {
        Map<String, ?> parameters = connection.getParameters();

        ConnectionDefinition connectionDefinition =
            connectionDefinitionService.getConnectionDefinition(connection.getComponentName(), 1);

        List<String> authorizationPropertyNames = connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();

        List<String> connectionPropertyNames = connectionDefinition.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .toList();

        return new ConnectionDTO(
            active,
            getAuthorizationParameters(parameters, authorizationPropertyNames),
            connection,
            getConnectionParameters(parameters, connectionPropertyNames),
            tags);
    }

    private static Map<String, ?> getAuthorizationParameters(
        Map<String, ?> parameters, List<String> authorizationPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> authorizationPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, ?> getConnectionParameters(
        Map<String, ?> parameters, List<String> connectionPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> connectionPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
