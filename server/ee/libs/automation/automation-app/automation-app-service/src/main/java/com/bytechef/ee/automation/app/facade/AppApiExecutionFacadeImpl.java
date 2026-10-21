/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.dto.AppApiCollectionDTO;
import com.bytechef.ee.automation.app.dto.AppApiEndpointDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
import com.bytechef.ee.automation.app.workflow.AppWorkflowSchemas;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Resolves the API collections an app may call through its bindings and records each call. Collections and endpoints
 * are read through API Platform services: an app key holds no user permissions, so the binding is the authorization.
 * The facade is deliberately not transactional: the endpoint workflow must not run inside a database transaction.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AppApiExecutionFacadeImpl implements AppApiExecutionFacade {

    private static final String API_PLATFORM_TRIGGER_COMPONENT_NAME = "apiPlatform";
    private static final String API_PLATFORM_TRIGGER_OPERATION_NAME = "newApiRequest";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionService apiCollectionService;
    private final AppApiCollectionResolver appApiCollectionResolver;
    private final AppApiCollectionService appApiCollectionService;
    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ExternalInvocationRecorder toolExecutionRecorder;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AppApiExecutionFacadeImpl(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionService apiCollectionService,
        AppApiCollectionResolver appApiCollectionResolver, AppApiCollectionService appApiCollectionService,
        AppInstanceService appInstanceService, AppService appService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ExternalInvocationRecorder toolExecutionRecorder, WorkflowService workflowService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionService = apiCollectionService;
        this.appApiCollectionResolver = appApiCollectionResolver;
        this.appApiCollectionService = appApiCollectionService;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.workflowService = workflowService;
    }

    @Override
    public List<ApiCollection> getCallableApiCollections() {
        AppInstance appInstance = resolveAppInstance();

        List<ApiCollection> callableApiCollections = new ArrayList<>();

        for (AppApiCollection binding : appApiCollectionService.getBindings(appInstance.getId())) {
            resolve(appInstance, binding).ifPresent(callableApiCollections::add);
        }

        return callableApiCollections;
    }

    @Override
    public List<ApiCollection> getUnavailableApiCollections() {
        AppInstance appInstance = resolveAppInstance();

        List<ApiCollection> unavailableApiCollections = new ArrayList<>();

        for (AppApiCollection binding : appApiCollectionService.getBindings(appInstance.getId())) {
            if (resolve(appInstance, binding).isEmpty()) {
                apiCollectionService.fetchApiCollection(binding.getApiCollectionId())
                    .ifPresent(unavailableApiCollections::add);
            }
        }

        return unavailableApiCollections;
    }

    @Override
    public List<AppApiCollectionDTO> getApiCollections() {
        AppInstance appInstance = resolveAppInstance();

        List<AppApiCollectionDTO> appApiCollectionDTOs = new ArrayList<>();

        for (AppApiCollection binding : appApiCollectionService.getBindings(appInstance.getId())) {
            Optional<ApiCollection> apiCollection = resolve(appInstance, binding);

            if (apiCollection.isPresent()) {
                appApiCollectionDTOs.add(toAvailableDTO(apiCollection.get()));
            } else {
                appApiCollectionDTOs.add(toUnavailableDTO(binding.getApiCollectionId()));
            }
        }

        return appApiCollectionDTOs;
    }

    @Override
    public <T> T recordApiCall(
        String collectionName, String endpointName, Supplier<T> call, ToIntFunction<T> statusOf) {

        AppPrincipal appPrincipal = AppPrincipal.getCurrent();

        AppInstance appInstance = resolveAppInstance(appPrincipal);

        ExternalInvocationEvent.Builder eventBuilder = ExternalInvocationEvent
            .builder(PlatformType.AUTOMATION, ExternalInvocationSource.APP_API, ExternalInvocationKind.WORKFLOW,
                endpointName)
            .appId(appInstance.getAppId())
            .appInstanceId(appInstance.getId())
            .userId(appPrincipal.userId())
            .workspaceId(appInstance.getWorkspaceId())
            .environment(appInstance.getEnvironmentId())
            .operationName(collectionName);

        return toolExecutionRecorder.recordWithErrorMessage(
            eventBuilder, call, result -> {
                int status = statusOf.applyAsInt(result);

                return status >= 400 ? "HTTP " + status : null;
            });
    }

    private Optional<Map<String, ?>> fetchTriggerParameters(ApiCollectionEndpoint apiCollectionEndpoint) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                apiCollectionEndpoint.getProjectDeploymentWorkflowId());

        Optional<Workflow> workflow = workflowService.fetchWorkflow(projectDeploymentWorkflow.getWorkflowId());

        return workflow.flatMap(curWorkflow -> WorkflowTrigger.of(curWorkflow)
            .stream()
            .filter(AppApiExecutionFacadeImpl::isApiPlatformTrigger)
            .findFirst())
            .map(WorkflowTrigger::getParameters);
    }

    private static boolean isApiPlatformTrigger(WorkflowTrigger workflowTrigger) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        return Objects.equals(workflowNodeType.name(), API_PLATFORM_TRIGGER_COMPONENT_NAME) &&
            Objects.equals(workflowNodeType.operation(), API_PLATFORM_TRIGGER_OPERATION_NAME);
    }

    /**
     * Parses each JSON Schema string of a {@code request} or {@code response} trigger property. Null when the property
     * is not an object or any of its schemas is not valid JSON.
     */
    private static @Nullable Map<String, Object> readSchemas(@Nullable Object schemas) {
        if (!(schemas instanceof Map<?, ?> schemaMap)) {
            return null;
        }

        Map<String, Object> parsedSchemas = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : schemaMap.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String jsonSchema) {
                try {
                    Map<String, Object> parsedSchema = AppWorkflowSchemas.parse(jsonSchema);

                    if (parsedSchema != null) {
                        parsedSchemas.put(String.valueOf(entry.getKey()), parsedSchema);
                    }
                } catch (IllegalArgumentException illegalArgumentException) {
                    return null;
                }
            } else if (value != null) {
                parsedSchemas.put(String.valueOf(entry.getKey()), value);
            }
        }

        return parsedSchemas;
    }

    private Optional<ApiCollection> resolve(AppInstance appInstance, AppApiCollection binding) {
        return appApiCollectionResolver.resolve(
            appInstance.getWorkspaceId(), appInstance.getEnvironment(), binding.getApiCollectionId());
    }

    private AppInstance resolveAppInstance() {
        return resolveAppInstance(AppPrincipal.getCurrent());
    }

    private AppInstance resolveAppInstance(AppPrincipal appPrincipal) {
        AppInstance appInstance = appInstanceService.getAppInstance(appPrincipal.appInstanceId());
        App app = appService.getApp(appInstance.getAppId());

        if (!AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus())) {
            throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
        }

        return appInstance;
    }

    private AppApiCollectionDTO toAvailableDTO(ApiCollection apiCollection) {
        List<AppApiEndpointDTO> appApiEndpointDTOs = new ArrayList<>();

        for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpointService.getApiEndpoints(
            apiCollection.getId())) {

            Map<String, ?> triggerParameters = fetchTriggerParameters(apiCollectionEndpoint).orElse(Map.of());
            HttpMethod httpMethod = apiCollectionEndpoint.getHttpMethod();

            appApiEndpointDTOs.add(
                new AppApiEndpointDTO(
                    apiCollectionEndpoint.getName(), httpMethod.name(), apiCollectionEndpoint.getPath(),
                    readSchemas(triggerParameters.get(REQUEST)), readSchemas(triggerParameters.get(RESPONSE))));
        }

        Integer collectionVersion = apiCollection.getCollectionVersion();

        return new AppApiCollectionDTO(
            apiCollection.getId(), apiCollection.getName(), apiCollection.getContextPath(),
            collectionVersion == null ? 0 : collectionVersion, true, appApiEndpointDTOs);
    }

    private AppApiCollectionDTO toUnavailableDTO(long apiCollectionId) {
        return apiCollectionService.fetchApiCollection(apiCollectionId)
            .map(apiCollection -> {
                Integer collectionVersion = apiCollection.getCollectionVersion();

                return new AppApiCollectionDTO(
                    apiCollectionId, apiCollection.getName(), apiCollection.getContextPath(),
                    collectionVersion == null ? 0 : collectionVersion, false, List.<AppApiEndpointDTO>of());
            })
            .orElseGet(
                () -> new AppApiCollectionDTO(apiCollectionId, "#" + apiCollectionId, null, 0, false, List.of()));
    }
}
