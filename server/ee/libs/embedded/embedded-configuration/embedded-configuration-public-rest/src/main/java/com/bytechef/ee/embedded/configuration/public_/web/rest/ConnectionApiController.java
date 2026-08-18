/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateConnectionRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ReauthorizeConnectionRequestModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.ConnectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectionApiController implements ConnectionApi {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectedUserService connectedUserService;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectionApiController(
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectedUserService connectedUserService,
        ConversionService conversionService, EnvironmentService environmentService) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserService = connectedUserService;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public ResponseEntity<List<ConnectionModel>> getConnections(
        String externalUserId, String componentName, EnvironmentModel xEnvironment, List<Long> connectionIds) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        // The authenticated embedded principal's name IS the connected user's externalId (see
        // EmbeddedApiKeyAuthenticationProvider). The path externalUserId must match it, otherwise a caller
        // authenticated for one end user could read another end user's connections (IDOR).
        Environment environment = getEnvironment(xEnvironment);

        // TODO Move to facade

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade
                .getConnections(connectedUser.getId(), componentName, connectionIds == null ? List.of() : connectionIds)
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getFrontendConnections(
        String componentName, EnvironmentModel xEnvironment, List<Long> connectionIds) {

        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade
                .getConnections(connectedUser.getId(), componentName, connectionIds == null ? List.of() : connectionIds)
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ConnectionModel>> getAllFrontendConnections(EnvironmentModel xEnvironment) {
        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        return ResponseEntity.ok(
            connectedUserConnectionFacade.getConnections(connectedUser.getId(), null, List.of())
                .stream()
                .map(connectionDTO -> conversionService.convert(connectionDTO, ConnectionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Long> createFrontendConnection(
        String componentName, CreateConnectionRequestModel createConnectionRequestModel,
        EnvironmentModel xEnvironment) {

        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        String authorizationType = createConnectionRequestModel.getAuthorizationType();

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName(componentName)
            .connectionVersion(createConnectionRequestModel.getConnectionVersion())
            .authorizationType(authorizationType == null ? null : AuthorizationType.valueOf(authorizationType))
            .environmentId(getEnvironment(xEnvironment).ordinal())
            .name(createConnectionRequestModel.getName())
            .parameters(createConnectionRequestModel.getParameters())
            .build();

        return ResponseEntity.ok(
            connectedUserConnectionFacade.createConnectedUserConnection(connectedUser.getId(), connectionDTO));
    }

    @Override
    public ResponseEntity<Void> deleteFrontendConnection(Long id, EnvironmentModel xEnvironment) {
        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        try {
            connectedUserConnectionFacade.deleteConnectedUserConnection(connectedUser.getId(), id);
        } catch (ConfigurationException configurationException) {
            if (isConnectionInUse(configurationException)) {
                throw new ConnectionInUseException();
            }

            throw configurationException;
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> reauthorizeFrontendConnection(
        Long id, ReauthorizeConnectionRequestModel reauthorizeConnectionRequestModel, EnvironmentModel xEnvironment) {

        ConnectedUser connectedUser = getCurrentConnectedUser(xEnvironment);

        connectedUserConnectionFacade.reauthorizeConnectedUserConnection(
            connectedUser.getId(), id, reauthorizeConnectionRequestModel.getParameters());

        return ResponseEntity.noContent()
            .build();
    }

    /**
     * {@code deleteFrontendConnection}'s generated signature returns {@code ResponseEntity<Void>} (its OpenAPI 204
     * response declares no content, so the generator infers {@code Void} instead of {@code Object}), which leaves no
     * room to return a JSON body inline for the 409 case. {@code deleteFrontendConnection} catches
     * {@link ConfigurationException} itself and, for the {@code CONNECTION_IS_USED} case only, throws this narrow,
     * dedicated, unconditionally-409 exception instead of the caught one — any other {@link ConfigurationException} is
     * rethrown untouched from that method (not from an {@code @ExceptionHandler}, so it propagates normally) and keeps
     * the platform-wide {@code GlobalResponseEntityExceptionHandler}'s 400 {@code ProblemDetail}. A class-level handler
     * here that instead branched on every {@link ConfigurationException} would outrank that advice for every route on
     * this controller and — on a rethrow from inside an {@code @ExceptionHandler}, which
     * {@code ExceptionHandlerExceptionResolver} does not re-dispatch to the next resolver — degrade unrelated
     * connection errors (e.g. {@code INVALID_CONNECTION}, {@code CONNECTION_NOT_ACTIVE}) to a bare 500.
     */
    @ExceptionHandler(ConnectionInUseException.class)
    public ResponseEntity<Object> handleConnectionInUseException() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("reason", "CONNECTION_IS_USED"));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private ConnectedUser getCurrentConnectedUser(EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not authenticated");

        return connectedUserService.getConnectedUser(externalUserId, getEnvironment(xEnvironment));
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return xEnvironment == null ? Environment.PRODUCTION : environmentService.getEnvironment(xEnvironment.name());
    }

    private static boolean isConnectionInUse(ConfigurationException configurationException) {
        return Connection.class.equals(configurationException.getEntityClass())
            && configurationException.getErrorKey() == ConnectionErrorType.CONNECTION_IS_USED.getErrorKey();
    }

    /**
     * Controller-local signal thrown only from {@link #deleteFrontendConnection} for the {@code CONNECTION_IS_USED}
     * case; {@link #handleConnectionInUseException} maps it unconditionally to 409, so catching it can never widen to
     * catch (and mis-map) any other {@link ConfigurationException}.
     */
    private static final class ConnectionInUseException extends RuntimeException {

        private ConnectionInUseException() {
        }
    }
}
