/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingInput;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionMissingConnectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionMissingInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionProjectVersionInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionUpdateModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionVersionUpdateModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API collection endpoints of the automation public API.
 *
 * <p>
 * {@link ApiCollectionFacade} owns all authorization, so this controller carries no {@code @PreAuthorize}: it maps
 * models to and from DTOs. A field absent from an update means "keep the stored value". Every by-id route first
 * requires the API collection to be in the request's environment and answers an API collection of another environment
 * as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.ApiCollectionPublicApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ApiCollectionPublicApiController extends AbstractApiCollectionPublicApiController
    implements ApiCollectionApi {

    private final ApiCollectionFacade apiCollectionFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public ApiCollectionPublicApiController(
        ApiCollectionFacade apiCollectionFacade, EnvironmentService environmentService) {

        this.apiCollectionFacade = apiCollectionFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<ApiCollectionModel>> getApiCollections(
        Long workspaceId, @Nullable Long projectId, @Nullable EnvironmentModel xEnvironment) {

        Environment environment = ApiCollectionEnvironmentScope.resolveEnvironment(environmentService, xEnvironment);

        return ResponseEntity.ok(
            apiCollectionFacade.getApiCollections(workspaceId, (long) environment.ordinal(), projectId, null)
                .stream()
                .map(ApiCollectionPublicApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<ApiCollectionModel> createApiCollection(
        Long workspaceId, ApiCollectionInputModel apiCollectionInputModel, @Nullable EnvironmentModel xEnvironment) {

        if (apiCollectionInputModel.getContextPath()
            .startsWith("/")) {

            throw new ConfigurationException(
                "Context path must not start with a slash.", ApiCollectionErrorType.INVALID_CONTEXT_PATH);
        }

        Environment environment = ApiCollectionEnvironmentScope.resolveEnvironment(environmentService, xEnvironment);

        ApiCollectionDTO apiCollectionDTO = new ApiCollectionDTO(
            apiCollectionInputModel.getCollectionVersion(), apiCollectionInputModel.getContextPath(), null, null,
            apiCollectionInputModel.getDescription(), false, List.of(), environment, null, null, null,
            apiCollectionInputModel.getName(), null, apiCollectionInputModel.getProjectId(), null, 0,
            apiCollectionInputModel.getProjectVersion(), List.of(), 0);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toModel(apiCollectionFacade.createApiCollection(apiCollectionDTO)));
    }

    @Override
    public ResponseEntity<ApiCollectionModel> getApiCollection(Long id, @Nullable EnvironmentModel xEnvironment) {
        return ResponseEntity.ok(toModel(getApiCollectionInEnvironment(id, xEnvironment)));
    }

    @Override
    public ResponseEntity<ApiCollectionModel> updateApiCollection(
        Long id, ApiCollectionUpdateModel apiCollectionUpdateModel, @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionDTO apiCollectionDTO = getApiCollectionInEnvironment(id, xEnvironment);

        String name = apiCollectionUpdateModel.getName();
        String description = apiCollectionUpdateModel.getDescription();

        if (name != null || description != null) {
            apiCollectionDTO = apiCollectionFacade.updateApiCollection(
                new ApiCollectionDTO(
                    apiCollectionDTO.collectionVersion(), apiCollectionDTO.contextPath(), null, null,
                    description == null ? apiCollectionDTO.description() : description, apiCollectionDTO.enabled(),
                    List.of(), apiCollectionDTO.environment(), id, null, null,
                    name == null ? apiCollectionDTO.name() : name, null, apiCollectionDTO.projectId(), null,
                    apiCollectionDTO.projectDeploymentId(), apiCollectionDTO.projectVersion(),
                    apiCollectionDTO.tags(), apiCollectionDTO.version()));
        }

        return ResponseEntity.ok(toModel(apiCollectionDTO));
    }

    @Override
    public ResponseEntity<Void> deleteApiCollection(Long id, @Nullable EnvironmentModel xEnvironment) {
        getApiCollectionInEnvironment(id, xEnvironment);

        apiCollectionFacade.deleteApiCollection(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ApiCollectionVersionUpdateModel> updateApiCollectionProjectVersion(
        Long id, ApiCollectionProjectVersionInputModel apiCollectionProjectVersionInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        getApiCollectionInEnvironment(id, xEnvironment);

        ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
            apiCollectionFacade.updateApiCollectionProjectVersion(
                id, apiCollectionProjectVersionInputModel.getProjectVersion(),
                Boolean.TRUE.equals(apiCollectionProjectVersionInputModel.getRemoveMissingEndpoints()));

        return ResponseEntity.ok(
            new ApiCollectionVersionUpdateModel()
                .apiCollection(toModel(apiCollectionVersionUpdateDTO.apiCollection()))
                .disabledEndpointNames(apiCollectionVersionUpdateDTO.disabledEndpointNames())
                .missingConnections(
                    CollectionUtils.map(
                        apiCollectionVersionUpdateDTO.missingConnections(),
                        ApiCollectionPublicApiController::toMissingConnectionModel))
                .missingInputs(
                    CollectionUtils.map(
                        apiCollectionVersionUpdateDTO.missingInputs(),
                        ApiCollectionPublicApiController::toMissingInputModel))
                .removedEndpointNames(apiCollectionVersionUpdateDTO.removedEndpointNames()));
    }

    private ApiCollectionDTO getApiCollectionInEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        return ApiCollectionEnvironmentScope.getApiCollection(apiCollectionFacade, environmentService, id,
            xEnvironment);
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static ApiCollectionModel toModel(ApiCollectionDTO apiCollectionDTO) {
        return new ApiCollectionModel()
            .id(apiCollectionDTO.id())
            .name(apiCollectionDTO.name())
            .description(apiCollectionDTO.description())
            .contextPath(apiCollectionDTO.contextPath())
            .collectionVersion(apiCollectionDTO.collectionVersion())
            .projectId(apiCollectionDTO.projectId())
            .projectVersion(apiCollectionDTO.projectVersion())
            .environment(EnvironmentModel.valueOf(apiCollectionDTO.environment()
                .name()))
            .enabled(apiCollectionDTO.enabled())
            .createdDate(toOffsetDateTime(apiCollectionDTO.createdDate()))
            .lastModifiedDate(toOffsetDateTime(apiCollectionDTO.lastModifiedDate()));
    }

    private static ApiCollectionMissingConnectionModel toMissingConnectionModel(MissingConnection missingConnection) {
        return new ApiCollectionMissingConnectionModel()
            .componentName(missingConnection.componentName())
            .endpointName(missingConnection.endpointName())
            .key(missingConnection.key())
            .workflowNodeName(missingConnection.workflowNodeName());
    }

    private static ApiCollectionMissingInputModel toMissingInputModel(MissingInput missingInput) {
        return new ApiCollectionMissingInputModel()
            .endpointName(missingInput.endpointName())
            .inputName(missingInput.inputName());
    }
}
