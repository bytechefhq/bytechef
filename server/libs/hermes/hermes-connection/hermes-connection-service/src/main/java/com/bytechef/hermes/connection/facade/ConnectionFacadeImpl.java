
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.connection.WorkflowConnection;
import com.bytechef.hermes.connection.config.OAuth2Properties;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.dto.ConnectionDTO;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ConnectionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        OAuth2Properties oAuth2Properties, TagService tagService, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.oAuth2Properties = oAuth2Properties;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ConnectionDTO create(ConnectionDTO connectionDTO) {
        Connection connection = connectionDTO.toConnection();

        if (StringUtils.hasText(connection.getAuthorizationName()) &&
            connection.containsParameter(Authorization.CODE)) {

            // TODO add support for OAUTH2_AUTHORIZATION_CODE_PKCE

            Authorization.AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
                connection.getAuthorizationName(), connection.getComponentName(), connection.getConnectionVersion());

            if (authorizationType == Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                Authorization.AuthorizationCallbackResponse authorizationCallbackResponse = connectionDefinitionService
                    .executeAuthorizationCallback(
                        connection.getComponentName(), connection.getConnectionVersion(),
                        oAuth2Properties.checkPredefinedApp(connection.getComponentName(),
                            connection.getParameters()),
                        connection.getAuthorizationName(), oAuth2Properties.getRedirectUri());

                connection.putAllParameters(authorizationCallbackResponse.toMap());
            }
        }

        List<Tag> tags = checkTags(connectionDTO.tags());

        if (!tags.isEmpty()) {
            connection.setTags(tags);
        }

        return new ConnectionDTO(
            isConnectionUsed(connection.getComponentName(), connection.getConnectionVersion()),
            connectionService.create(connection), tags);
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
    public ConnectionDTO getConnection(Long id) {
        Connection connection = connectionService.getConnection(id);

        return new ConnectionDTO(
            isConnectionUsed(connection.getComponentName(), connection.getConnectionVersion()), connection,
            tagService.getTags(connection.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(List<String> componentNames, List<Long> tagIds) {
        List<Connection> connections = connectionService.getConnections(componentNames, tagIds);

        List<Tag> tags = tagService.getTags(connections.stream()
            .flatMap(connection -> connection.getTagIds()
                .stream())
            .filter(Objects::nonNull)
            .toList());

        return connections.stream()
            .map(connection -> new ConnectionDTO(
                isConnectionUsed(connection.getComponentName(), connection.getConnectionVersion()), connection,
                filterTags(tags, connection)))
            .toList();
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
    public ConnectionDTO update(Long id, List<Tag> tags) {
        tags = checkTags(tags);

        Connection connection = connectionService.update(
            id, com.bytechef.commons.util.CollectionUtils.map(tags, Tag::getId));

        return new ConnectionDTO(
            isConnectionUsed(connection.getComponentName(), connection.getConnectionVersion()), connection, tags);
    }

    @Override
    public ConnectionDTO update(ConnectionDTO connectionDTO) {
        List<Tag> tags = checkTags(connectionDTO.tags());

        return new ConnectionDTO(
            isConnectionUsed(connectionDTO.componentName(), connectionDTO.connectionVersion()),
            connectionService.update(
                connectionDTO.id(), connectionDTO.name(),
                com.bytechef.commons.util.CollectionUtils.map(tags, Tag::getId), connectionDTO.version()),
            tags);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags)
            ? Collections.emptyList()
            : tagService.save(tags);
    }

    private boolean isConnectionUsed(String componentName, int connectionVersion) {
        List<Workflow> workflows = workflowService.getWorkflows();

        for (Workflow workflow : workflows) {
            for (WorkflowTask workflowTask : workflow.getTasks()) {
                if (containsConnection(workflowTask, componentName, connectionVersion)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Boolean containsConnection(WorkflowTask workflowTask, String componentName, int connectionVersion) {
        return workflowTask
            .fetchExtension(WorkflowConnection.class)
            .map(workflowConnection -> Objects.equals(
                workflowConnection.componentName(), componentName) &&
                workflowConnection.connectionVersion() == connectionVersion)
            .orElse(false);
    }

    private List<Tag> filterTags(List<Tag> tags, Connection connection) {
        return tags.stream()
            .filter(
                tag -> {
                    List<Long> curTagIds = connection.getTagIds();

                    return curTagIds.contains(tag.getId());
                })
            .toList();
    }
}
