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

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;

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
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.domain.ResourceVisibility;
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

    private static final Logger log = LoggerFactory.getLogger(ConnectionFacadeImpl.class);

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
     * Overwrite the requested visibility, logging when the two differ so the divergence from the request body shows up
     * in audit/ops review rather than being silent.
     */
    private void forceVisibility(Connection connection, ResourceVisibility forced, PlatformType type) {
        ResourceVisibility requested = connection.getVisibility();

        if (requested != forced && log.isInfoEnabled()) {
            log.info(
                "Forcing {} visibility for connection (requested={}, platformType={}, eeEdition={})",
                forced, requested, type, eeEdition);
        }

        connection.setVisibility(forced);
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

        // Two overrides that ignore the request body entirely. The UI gates these surfaces too, but only a
        // facade-level override stops a hand-crafted request.
        //
        // EMBEDDED: a connection belongs to a connected user — an end user of a customer's product — not to a
        // workspace member, so "visible to the whole workspace" has no meaning and WORKSPACE would be wrong in a
        // way that crosses customers. PRIVATE is the value that cannot widen anything if a query reaches the row
        // by another path.
        //
        // CE: there is no authorization boundary between workspace members and no visibility picker, so every
        // connection is workspace-visible.
        if (type == PlatformType.EMBEDDED) {
            forceVisibility(connection, ResourceVisibility.PRIVATE, type);
        } else if (!eeEdition) {
            forceVisibility(connection, ResourceVisibility.WORKSPACE, type);
        }

        resolveOAuth2AuthorizationCode(connection);

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
    public void replaceAuthorizationParameters(long id, Map<String, ?> parameters) {
        Connection connection = getConnectionWithReplacedAuthorizationParameters(id, parameters);

        resolveOAuth2AuthorizationCode(connection);

        Map<String, ?> updatedParameters = new HashMap<>(connection.getParameters());

        updatedParameters.remove("state");

        connectionService.replaceConnectionParameters(id, updatedParameters);

        // Nothing else in production sets this back to VALID. Without it ComponentDefinitionFacadeImpl keeps blocking
        // execution on the connection, and ConnectionAfterSaveEventListener never re-arms its token refresh routine, so
        // a reconnect would report success and leave the connection just as unusable as before.
        connectionService.updateConnectionCredentialStatus(id, Connection.CredentialStatus.VALID);
    }

    /**
     * Builds the connection's complete post-replacement parameter map: every key the connection definition declares as
     * an authorization property is dropped, then {@code parameters} is applied on top. Subtracting by declared name is
     * what lets an unsubmitted optional credential actually clear while a connection-level property survives, without
     * the caller having to tell us which of its keys are credentials.
     */
    private Connection getConnectionWithReplacedAuthorizationParameters(long id, Map<String, ?> parameters) {
        Connection connection = connectionService.getConnection(id);

        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            connection.getComponentName(), connection.getConnectionVersion());

        List<String> authorizationPropertyNames = connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();

        Map<String, Object> replacedParameters = new HashMap<>(connection.getParameters());

        authorizationPropertyNames.forEach(replacedParameters::remove);

        replacedParameters.putAll(parameters);

        connection.setParameters(replacedParameters);

        return connection;
    }

    /**
     * Re-runs the OAuth2 authorization-code exchange when {@code connection} carries an authorization {@code code}
     * parameter, merging the callback result (and, when neither client id nor client secret survived the exchange, the
     * predefined client id/secret) back into {@code connection}'s parameters. Shared by {@link #create} and
     * {@link #replaceAuthorizationParameters} so a reconnect re-runs exactly the same exchange as the original connect.
     */
    private void resolveOAuth2AuthorizationCode(Connection connection) {
        if (connection.getAuthorizationType() != null && connection.containsParameter(Authorization.CODE)) {

            // TODO add support for OAUTH2_AUTHORIZATION_CODE_PKCE

            AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getAuthorizationType());

            if (authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE ||
                authorizationType == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

                Map<String, ?> predefinedParameters = oAuth2Service.checkPredefinedParameters(
                    connection.getComponentName(), connection.getParameters());

                AuthorizationCallbackResponse authorizationCallbackResponse = Objects.requireNonNull(
                    connectionDefinitionService.executeAuthorizationCallback(
                        connection.getComponentName(), connection.getConnectionVersion(),
                        connection.getAuthorizationType(), predefinedParameters,
                        oAuth2Service.getRedirectUri()),
                    "Authorization callback returned no response for component " + connection.getComponentName());

                connection.putAllParameters(authorizationCallbackResponse.result());

                Map<String, ?> parameters = connection.getParameters();

                Object clientId = parameters.get(CLIENT_ID);
                Object clientSecret = parameters.get(CLIENT_SECRET);

                if ((clientId == null || clientId.equals("")) && (clientSecret == null || clientSecret.equals(""))) {
                    connection.putAllParameters(
                        Map.of(
                            CLIENT_ID, predefinedParameters.get(CLIENT_ID),
                            CLIENT_SECRET, predefinedParameters.get(CLIENT_SECRET)));
                }

                if (log.isWarnEnabled() && !connection.containsParameter(Authorization.REFRESH_TOKEN)) {
                    log.warn(
                        "OAuth2 authorization code connection for component {} does not contain refresh token",
                        connection.getComponentName());
                }
            }
        }
    }

    @Override
    public Integer executeConnectionRefresh(Long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        ComponentConnection componentConnection = new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connection.getId(),
            connection.getParameters(), connection.getAuthorizationType());

        connectionDefinitionService.executeConnectionRefresh(componentConnection);

        connection = connectionService.getConnection(connectionId);

        Map<String, ?> parameters = connection.getParameters();

        log.info("Executed connection refresh for connection with connectionId: {}", connectionId);

        return (Integer) parameters.get("expires_in");
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
    public List<ConnectionDTO> getAiProviderConnections(
        String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        if (tagId != null) {
            return List.of();
        }

        List<Connection> connections = connectionService.getAiProviderConnections(
            componentName, connectionVersion, environmentId == null ? null : environmentId.intValue());

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
                    | NoSuchElementException exception) {
                    // Narrowed from a blanket `catch (Exception)` to the set that actually represents a
                    // DTO-mapping failure (missing component/connection definition, bad ordinal, invalid
                    // parameter). Spring security failures (AccessDeniedException) and data-access
                    // failures (DataAccessException) propagate so they are not silently hidden as
                    // "broken row" — those indicate systemic issues that deserve to surface to the
                    // caller rather than be metered-away. The full throwable is logged (dropping the
                    // stack trace via log.error(message) would have been forbidden), and the
                    // bytechef_connection_list_dto_failed counter lets operators alert on a rising
                    // skip rate rather than chasing individual log lines.
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Failed to build ConnectionDTO for connection id={} componentName={}; "
                                + "returning a degraded placeholder so admins can see the row exists",
                            connectionId, connection.getComponentName(), exception);
                    } else {
                        log.warn(
                            "Failed to build ConnectionDTO for connection id={} componentName={}; "
                                + "returning a degraded placeholder so admins can see the row exists",
                            connectionId, connection.getComponentName());
                    }

                    if (meterRegistry != null) {
                        Counter.builder("bytechef_connection_list_dto_failed")
                            .description("Number of connections returned as degraded placeholders from list "
                                + "responses due to DTO mapping errors")
                            .tag("componentName", String.valueOf(connection.getComponentName()))
                            .register(meterRegistry)
                            .increment();
                    }

                    return buildDegradedConnectionDTO(connection, filterTags(tags, connection), exception);
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
            getConnectionParameters(parameters, connectionPropertyNames), tags);
    }

    private String getBaseUri(
        Connection connection, String componentName, int connectionVersion, Map<String, ?> parameters) {

        String uri = null;

        try {
            ComponentConnection componentConnection = new ComponentConnection(
                componentName, connectionVersion, connection.getId(), parameters,
                connection.getAuthorizationType());

            uri = connectionDefinitionService.executeBaseUri(componentName, componentConnection)
                .orElse(null);
        } catch (Exception exception) {
            log.warn(
                "Failed to compute baseUri for connection id={} componentName={}; returning null",
                connection.getId(), componentName, exception);
        }

        return uri;
    }
}
