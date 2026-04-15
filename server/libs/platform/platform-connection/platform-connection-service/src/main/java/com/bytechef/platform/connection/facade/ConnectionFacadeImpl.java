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
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean eeEdition;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final MeterRegistry meterRegistry;
    private final OAuth2Service oAuth2Service;
    private final TagService tagService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI2"
    })
    public ConnectionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        @Value("${bytechef.edition:CE}") String edition,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, OAuth2Service oAuth2Service, TagService tagService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        validateEdition(edition);
        this.eeEdition = "EE".equalsIgnoreCase(edition);
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.oAuth2Service = oAuth2Service;
        this.tagService = tagService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    /**
     * Fail loud at startup when {@code bytechef.edition} is set to an unknown value so an accidental misconfiguration
     * (e.g. {@code Enterprise}, {@code ee }, a typo) does not silently disable EE features by falling through the
     * {@code "EE".equalsIgnoreCase(edition)} check.
     */
    private static void validateEdition(String edition) {
        if (edition == null
            || !("CE".equalsIgnoreCase(edition) || "EE".equalsIgnoreCase(edition))) {
            throw new IllegalStateException(
                "bytechef.edition must be CE or EE (case-insensitive); got '" + edition + "'");
        }
    }

    @Override
    public long create(ConnectionDTO connectionDTO, PlatformType type) {
        Connection connection = connectionDTO.toConnection();

        // Visibility is an EE+automation feature: force PRIVATE for embedded paths and on CE editions
        // regardless of incoming value. UI gates these surfaces too, but defense-in-depth at the facade
        // protects against hand-crafted requests. Log when a requested non-PRIVATE value is downgraded
        // so the divergence from the request body is visible in audit/ops review rather than silent.
        if (type == PlatformType.EMBEDDED || !eeEdition) {
            ConnectionVisibility requested = connection.getVisibility();

            if (requested != ConnectionVisibility.PRIVATE && logger.isInfoEnabled()) {
                logger.info(
                    "Forcing PRIVATE visibility for connection (requested={}, platformType={}, eeEdition={})",
                    requested, type, eeEdition);
            }

            connection.setVisibility(ConnectionVisibility.PRIVATE);
        }

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
    public List<ConnectionDTO> getConnections(List<Long> connectionIds, PlatformType type) {
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
        PlatformType type) {

        List<Connection> connections = CollectionUtils.filter(
            connectionService.getConnections(
                componentName, connectionVersion, tagId, environmentId, type),
            connection -> connectionIds.isEmpty() || connectionIds.contains(connection.getId()));

        return getConnections(connections);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getConnectionTags(PlatformType type) {
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

    private boolean isConnectionUsed(long connectionId, PlatformType type) {
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

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> toConnectionDTOs(List<Connection> connections) {
        return getConnections(connections);
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
                Long connectionId = connection.getId();

                try {
                    return toConnectionDTO(
                        isConnectionUsed(Validate.notNull(connectionId, "id"), connection.getType()), connection,
                        filterTags(tags, connection));
                } catch (IllegalStateException | IllegalArgumentException
                    | NoSuchElementException mappingError) {
                    // Narrowed from a blanket `catch (Exception)` to the set that actually represents a
                    // DTO-mapping failure (missing component/connection definition, bad ordinal, invalid
                    // parameter). Spring security failures (AccessDeniedException) and data-access
                    // failures (DataAccessException) propagate so they are not silently hidden as
                    // "broken row" — those indicate systemic issues that deserve to surface to the
                    // caller rather than be metered-away. The full throwable is logged (dropping the
                    // stack trace via logger.error(message) would have been forbidden), and the
                    // bytechef_connection_list_dto_failed counter lets operators alert on a rising
                    // skip rate rather than chasing individual log lines.
                    logger.error(
                        "Failed to build ConnectionDTO for connection id={} componentName={}; "
                            + "returning a degraded placeholder so admins can see the row exists",
                        connectionId, connection.getComponentName(), mappingError);

                    if (meterRegistry != null) {
                        Counter.builder("bytechef_connection_list_dto_failed")
                            .description("Number of connections returned as degraded placeholders from list "
                                + "responses due to DTO mapping errors")
                            .tag("componentName", String.valueOf(connection.getComponentName()))
                            .register(meterRegistry)
                            .increment();
                    }

                    return buildDegradedConnectionDTO(connection, filterTags(tags, connection), mappingError);
                }
            })
            .toList();
    }

    /**
     * Builds a best-effort placeholder DTO when {@link #toConnectionDTO} throws. The previous behavior — returning
     * {@code null} and filtering it out — made failing rows invisible to admins; they could not tell a mapping failure
     * from a missing connection. A degraded DTO surfaces the row with {@code active=false}, an empty authorization
     * payload, and {@code baseUri=null}, so the UI can render the connection with enough context (id, name,
     * componentName, credentialStatus, status, visibility, tags) for an operator to investigate or delete it.
     *
     * <p>
     * Derived fields that depend on the component registry ({@code authorizationType}, {@code authorizationParameters},
     * {@code connectionParameters}, {@code baseUri}) are intentionally zeroed — the throw that brought us here proves
     * we cannot resolve them. Parameters are wiped rather than passed through so a corrupted row cannot leak raw
     * credential material to a list surface that normally filters it through the component definition.
     */
    private static ConnectionDTO buildDegradedConnectionDTO(Connection connection, List<Tag> tags, Throwable cause) {
        // The Builder hardcodes authorizationParameters and connectionParameters to Map.of() in its
        // build() step, which is exactly what we want here — a corrupted row must not leak raw
        // credential material to a list surface that normally filters it through the component
        // definition. `parameters` is also left null (the Builder's default) for the same reason.
        return ConnectionDTO.builder()
            .active(false)
            .authorizationType(null)
            .baseUri(null)
            .componentName(connection.getComponentName())
            .connectionVersion(connection.getConnectionVersion())
            .createdBy(connection.getCreatedBy())
            .createdDate(connection.getCreatedDate())
            .credentialStatus(connection.getCredentialStatus())
            .environmentId(connection.getEnvironmentId())
            .id(connection.getId())
            .lastModifiedBy(connection.getLastModifiedBy())
            .lastModifiedDate(connection.getLastModifiedDate())
            .name(degradedName(connection, cause))
            .sharedProjectIds(List.of())
            .status(connection.getStatus())
            .tags(tags)
            .version(connection.getVersion())
            .visibility(connection.getVisibility())
            .build();
    }

    /**
     * Annotates the connection name with a short suffix so operators visually distinguish a degraded row from a healthy
     * one in the listing, without changing the persisted name. Kept short to avoid blowing up table layouts.
     */
    private static String degradedName(Connection connection, Throwable cause) {
        String suffix = " [unavailable: " + cause.getClass()
            .getSimpleName() + "]";
        String name = connection.getName();

        return name == null ? suffix.trim() : name + suffix;
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
            getConnectionParameters(parameters, connectionPropertyNames), List.of(), tags);
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
        } catch (IllegalStateException exception) {
            // WARN with the full throwable (was: DEBUG with only the message). baseUri resolution
            // failures mean the admin UI renders the connection with an empty base URI and — until
            // this log upgrade — operators had no visibility into which components were failing.
            // Causes include misconfigured component registration, encryption failures on stored
            // parameters, or corruption in the connection-definition registry; none are benign.
            logger.warn(
                "Failed to compute baseUri for connection id={} componentName={}; returning null",
                connection.getId(), componentName, exception);
        }

        return uri;
    }
}
