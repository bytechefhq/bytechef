/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionEndpointEnabledInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionEndpointInputModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionEndpointModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.ApiCollectionEndpointUpdateModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EndpointConnectionModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model.HttpMethodModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API collection endpoint routes of the automation public API.
 *
 * <p>
 * {@link ApiCollectionFacade} owns all authorization, so this controller carries no {@code @PreAuthorize}: it maps
 * models to and from DTOs. A field absent from an update means "keep the stored value", the endpoint's connections and
 * inputs included; only a present field replaces. Every route first requires the API collection of the endpoint to be
 * in the request's environment and answers an API collection of another environment as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.ApiCollectionEndpointPublicApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ApiCollectionEndpointPublicApiController extends AbstractApiCollectionPublicApiController
    implements ApiCollectionEndpointApi {

    private final ApiCollectionFacade apiCollectionFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public ApiCollectionEndpointPublicApiController(
        ApiCollectionFacade apiCollectionFacade, EnvironmentService environmentService) {

        this.apiCollectionFacade = apiCollectionFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<ApiCollectionEndpointModel>> getApiCollectionEndpoints(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionEnvironmentScope.getApiCollection(apiCollectionFacade, environmentService, id, xEnvironment);

        return ResponseEntity.ok(
            apiCollectionFacade.getApiCollectionEndpoints(id)
                .stream()
                .map(ApiCollectionEndpointPublicApiController::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> createApiCollectionEndpoint(
        Long id, ApiCollectionEndpointInputModel apiCollectionEndpointInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionEnvironmentScope.getApiCollection(apiCollectionFacade, environmentService, id, xEnvironment);

        ApiCollectionEndpointDTO apiCollectionEndpointDTO = new ApiCollectionEndpointDTO(
            id, null, null, false, toHttpMethod(apiCollectionEndpointInputModel.getHttpMethod()), null, null, null,
            apiCollectionEndpointInputModel.getName(), apiCollectionEndpointInputModel.getPath(), 0, 0,
            apiCollectionEndpointInputModel.getWorkflowUuid());

        List<EndpointConnectionModel> endpointConnectionModels = apiCollectionEndpointInputModel.getConnections();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                toModel(
                    apiCollectionFacade.createApiCollectionEndpoint(
                        apiCollectionEndpointDTO,
                        endpointConnectionModels == null ? null : toConnections(endpointConnectionModels),
                        apiCollectionEndpointInputModel.getInputs())));
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> getApiCollectionEndpoint(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(toModel(getEndpointInEnvironment(id, xEnvironment)));
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> updateApiCollectionEndpoint(
        Long id, ApiCollectionEndpointUpdateModel apiCollectionEndpointUpdateModel,
        @Nullable EnvironmentModel xEnvironment) {

        ApiCollectionEndpointDTO apiCollectionEndpointDTO = getEndpointInEnvironment(id, xEnvironment);

        List<EndpointConnectionModel> endpointConnectionModels = apiCollectionEndpointUpdateModel.getConnections();
        Map<String, Object> inputs = apiCollectionEndpointUpdateModel.getInputs();

        if (hasFieldChange(apiCollectionEndpointUpdateModel) || endpointConnectionModels != null || inputs != null) {
            HttpMethodModel httpMethod = apiCollectionEndpointUpdateModel.getHttpMethod();
            String name = apiCollectionEndpointUpdateModel.getName();
            String path = apiCollectionEndpointUpdateModel.getPath();

            apiCollectionEndpointDTO = apiCollectionFacade.updateApiCollectionEndpoint(
                new ApiCollectionEndpointDTO(
                    apiCollectionEndpointDTO.apiCollectionId(), null, null, false,
                    httpMethod == null ? apiCollectionEndpointDTO.httpMethod() : toHttpMethod(httpMethod), id, null,
                    null, name == null ? apiCollectionEndpointDTO.name() : name,
                    path == null ? apiCollectionEndpointDTO.path() : path, 0, 0,
                    apiCollectionEndpointUpdateModel.getWorkflowUuid()),
                endpointConnectionModels == null ? null : toConnections(endpointConnectionModels), inputs);
        }

        return ResponseEntity.ok(toModel(apiCollectionEndpointDTO));
    }

    @Override
    public ResponseEntity<Void> deleteApiCollectionEndpoint(Long id, @Nullable EnvironmentModel xEnvironment) {
        getEndpointInEnvironment(id, xEnvironment);

        apiCollectionFacade.deleteApiCollectionEndpoint(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> enableApiCollectionEndpoint(
        Long id, ApiCollectionEndpointEnabledInputModel apiCollectionEndpointEnabledInputModel,
        @Nullable EnvironmentModel xEnvironment) {

        getEndpointInEnvironment(id, xEnvironment);

        return ResponseEntity.ok(
            toModel(
                apiCollectionFacade.enableApiCollectionEndpoint(id,
                    apiCollectionEndpointEnabledInputModel.getEnabled())));
    }

    private ApiCollectionEndpointDTO getEndpointInEnvironment(long id, @Nullable EnvironmentModel xEnvironment) {
        return ApiCollectionEnvironmentScope.getApiCollectionEndpoint(
            apiCollectionFacade, environmentService, id, xEnvironment);
    }

    private static boolean hasFieldChange(ApiCollectionEndpointUpdateModel apiCollectionEndpointUpdateModel) {
        return apiCollectionEndpointUpdateModel.getName() != null ||
            apiCollectionEndpointUpdateModel.getHttpMethod() != null ||
            apiCollectionEndpointUpdateModel.getPath() != null ||
            apiCollectionEndpointUpdateModel.getWorkflowUuid() != null;
    }

    private static HttpMethod toHttpMethod(HttpMethodModel httpMethodModel) {
        return HttpMethod.valueOf(httpMethodModel.name());
    }

    private static List<ProjectDeploymentWorkflowConnection> toConnections(
        List<EndpointConnectionModel> endpointConnectionModels) {

        return CollectionUtils.map(
            endpointConnectionModels,
            endpointConnectionModel -> new ProjectDeploymentWorkflowConnection(
                endpointConnectionModel.getConnectionId(), endpointConnectionModel.getKey(),
                endpointConnectionModel.getWorkflowNodeName()));
    }

    private static ApiCollectionEndpointModel toModel(ApiCollectionEndpointDTO apiCollectionEndpointDTO) {
        return new ApiCollectionEndpointModel()
            .id(apiCollectionEndpointDTO.id())
            .apiCollectionId(apiCollectionEndpointDTO.apiCollectionId())
            .name(apiCollectionEndpointDTO.name())
            .httpMethod(HttpMethodModel.valueOf(apiCollectionEndpointDTO.httpMethod()
                .name()))
            .path(apiCollectionEndpointDTO.path())
            .workflowUuid(apiCollectionEndpointDTO.workflowUuid())
            .enabled(apiCollectionEndpointDTO.enabled())
            .connections(
                CollectionUtils.map(
                    apiCollectionEndpointDTO.connections(),
                    connection -> new EndpointConnectionModel(
                        connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey(),
                        connection.getConnectionId())))
            .inputs(new LinkedHashMap<>(apiCollectionEndpointDTO.inputs()));
    }
}
