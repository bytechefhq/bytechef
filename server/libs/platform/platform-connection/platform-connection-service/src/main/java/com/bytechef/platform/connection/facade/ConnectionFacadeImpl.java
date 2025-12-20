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

package com.bytechef.platform.connection.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.oauth2.service.OAuth2Service;
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
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectionFacadeImpl implements ConnectionFacade {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionFacadeImpl.class);

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI2")
    public ConnectionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, OAuth2Service oAuth2Service, TagService tagService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public long create(ConnectionDTO connectionDTO, ModeType type) {
        Connection connection = connectionDTO.toConnection();

        if (connection.getAuthorizationType() != null && connection.containsParameter(Authorization.CODE)) {

            // TODO add support for OAUTH2_AUTHORIZATION_CODE_PKCE

            AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getAuthorizationType());

            if (authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                AuthorizationCallbackResponse authorizationCallbackResponse =
                    connectionDefinitionService.executeAuthorizationCallback(
                        connection.getComponentName(), connection.getConnectionVersion(),
                        connection.getAuthorizationType(),
                        oAuth2Service.checkPredefinedParameters(
                            connection.getComponentName(), connection.getParameters()),
                        oAuth2Service.getRedirectUri());

                connection.putAllParameters(authorizationCallbackResponse.result());

                if (logger.isWarnEnabled() && !connection.containsParameter(Authorization.REFRESH_TOKEN)) {
                    logger.warn(
                        "OAuth2 authorization code connection for component {} does not contain refresh token",
                        connection.getComponentName());
                }
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

        return connection.getId();
    }

    @Override
    public void delete(Long id) {
        Connection connection = connectionService.getConnection(id);

        if (isConnectionUsed(id, connection.getType())) {
            throw new ConfigurationException(
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
    public List<ConnectionDTO> getConnections(List<Long> connectionIds, ModeType type) {
        return connectionService.getConnections(connectionIds)
            .stream()
            .map(connection -> toConnectionDTO(
                isConnectionUsed(Validate.notNull(connection.getId(), "id"), type), connection, List.of()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        ModeType type) {

        List<Connection> connections = CollectionUtils.filter(
            connectionService.getConnections(
                componentName, connectionVersion, tagId, environmentId, type),
            connection -> connectionIds.isEmpty() || connectionIds.contains(connection.getId()));

        return getConnections(connections);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getConnectionTags(ModeType type) {
        List<Connection> connections = connectionService.getConnections(type);

        return tagService.getTags(
            connections
                .stream()
                .map(Connection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public void update(long id, List<Tag> tags) {
        tags = checkTags(tags);

        connectionService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public void update(long id, String name, List<Tag> tags, int version) {
        tags = checkTags(tags);

        connectionService.update(id, name, CollectionUtils.map(tags, Tag::getId), version);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(Connection connection, Tag tag) {
        List<Long> curTagIds = connection.getTagIds();

        return curTagIds.contains(tag.getId());
    }

    private boolean isConnectionUsed(long connectionId, ModeType type) {
        boolean connectionUsed;

        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

        connectionUsed = jobPrincipalAccessor.isConnectionUsed(connectionId);

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

    private static Map<String, ?> getAuthorizationParameters(
        Map<String, ?> parameters, List<String> authorizationPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> authorizationPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<ConnectionDTO> getConnections(List<Connection> connections) {
        List<Tag> tags = tagService.getTags(
            connections
                .stream()
                .flatMap(connection -> CollectionUtils.stream(connection.getTagIds()))
                .filter(Objects::nonNull)
                .toList());

        return connections.stream()
            .map(connection -> {
                try {
                    return toConnectionDTO(
                        isConnectionUsed(Validate.notNull(connection.getId(), "id"), connection.getType()), connection,
                        filterTags(tags, connection));
                } catch (Exception e) {
                    logger.error(e.getMessage());

                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static Map<String, ?> getConnectionParameters(
        Map<String, ?> parameters, List<String> connectionPropertyNames) {

        return parameters.entrySet()
            .stream()
            .filter(entry -> connectionPropertyNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ConnectionDTO toConnectionDTO(boolean active, Connection connection, List<Tag> tags) {
        Map<String, ?> parameters = connection.getParameters();
        String componentName = connection.getComponentName();
        int connectionVersion = connection.getConnectionVersion();

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            componentName, connectionVersion);

        List<String> authorizationPropertyNames = connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();
        List<String> connectionPropertyNames = connectionDefinition.getProperties()
            .stream()
            .map(BaseProperty::getName)
            .toList();
        Map<String, ?> predefinedParameters = oAuth2Service.checkPredefinedParameters(componentName, parameters);
        String baseUri = getBaseUri(connection, componentName, connectionVersion, parameters);

        return new ConnectionDTO(
            active, getAuthorizationParameters(predefinedParameters, authorizationPropertyNames), baseUri, connection,
            getConnectionParameters(parameters, connectionPropertyNames), tags);
    }

    private String getBaseUri(
        Connection connection, String componentName, int connectionVersion, Map<String, ?> parameters) {

        String uri = null;

        try {
            ComponentConnection componentConnection = new ComponentConnection(
                componentName, connectionVersion, connection.getId(), parameters,
                connection.getAuthorizationType());

            uri = connectionDefinitionService
                .executeBaseUri(componentName, componentConnection)
                .orElse(null);
        } catch (IllegalStateException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage());
            }
        }

        return uri;
    }
}
