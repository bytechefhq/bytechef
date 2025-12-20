/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(ApiPlatformHandlerController.API_PLATFORM_BASE_PATH + "/**")
@CrossOrigin
@ConditionalOnCoordinator
public class ApiPlatformHandlerController extends AbstractWebhookTriggerController {

    protected static final String API_PLATFORM_BASE_PATH = "/api/o";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ApiPlatformHandlerController(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ApplicationProperties applicationProperties, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TempFileStorage tempFileStorage,
        TriggerDefinitionService triggerDefinitionService, WebhookWorkflowExecutor webhookWorkflowExecutor,
        WorkflowService workflowService) {

        super(
            jobPrincipalAccessorRegistry, applicationProperties.getPublicUrl(), tempFileStorage,
            triggerDefinitionService, webhookWorkflowExecutor, workflowService);

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @DeleteMapping(produces = "application/json")
    public ResponseEntity<?> handleDeleteMethod(
        final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        return doHandle(HttpMethod.DELETE, httpServletRequest, httpServletResponse);
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<?> handleGetMethod(
        final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        return doHandle(HttpMethod.GET, httpServletRequest, httpServletResponse);
    }

    @PatchMapping(produces = "application/json")
    public ResponseEntity<?> handlePatchMethod(
        final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        return doHandle(HttpMethod.PATCH, httpServletRequest, httpServletResponse);
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<?> handlePostMethod(
        final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        return doHandle(HttpMethod.POST, httpServletRequest, httpServletResponse);
    }

    @PutMapping(produces = "application/json")
    public ResponseEntity<?> handlePutMethod(
        final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

        return doHandle(HttpMethod.PUT, httpServletRequest, httpServletResponse);
    }

    private ResponseEntity<Object> doHandle(
        HttpMethod httpMethod, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        return TenantContext.callWithTenantId(TenantContext.getCurrentTenantId(), () -> {
            Map<String, List<String>> variables;

            String requestURI = httpServletRequest.getRequestURI();

            String path = requestURI.replace(API_PLATFORM_BASE_PATH, "");

            ApiCollectionEndpointResult apiCollectionEndpointResult = getApiCollectionEndpoint(
                httpMethod, path, getEnvironment(httpServletRequest));

            ApiCollection apiCollection = apiCollectionEndpointResult.apiCollection;
            ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointResult.apiCollectionEndpoint;

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    apiCollectionEndpoint.getProjectDeploymentWorkflowId());

            if (!projectDeploymentWorkflow.isEnabled()) {
                return ResponseEntity.status(404)
                    .body("API Collection Endpoint is not enabled");
            }

            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                apiCollection.getProjectDeploymentId());

            if (!projectDeployment.isEnabled()) {
                return ResponseEntity.status(404)
                    .body("API Collection is not enabled");
            }

            variables = PATH_MATCHER
                .extractUriTemplateVariables(
                    getPathPattern(
                        apiCollection.getContextPath(), apiCollection.getCollectionVersion(),
                        apiCollectionEndpoint.getPath()),
                    path)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

            // TODO fetch from New API Request Trigger
            WebhookTriggerFlags webhookTriggerFlags = new WebhookTriggerFlags(false, true, false, false);

            WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

            webhookRequest = new WebhookRequest(
                webhookRequest.headers(), MapUtils.concat(webhookRequest.parameters(), variables),
                webhookRequest.body(), webhookRequest.method());

            String workflowUuid = projectWorkflowService.getProjectDeploymentWorkflowUuid(
                projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId());

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                ModeType.AUTOMATION, apiCollection.getProjectDeploymentId(), workflowUuid, "trigger_1");

            return doProcessTrigger(workflowExecutionId, webhookRequest, httpServletRequest, httpServletResponse);
        });
    }

    private ApiCollectionEndpointResult getApiCollectionEndpoint(
        HttpMethod httpMethod, String path, Environment environment) {

        for (ApiCollection apiCollection : apiCollectionService.getApiCollections(null, environment, null, null)) {
            List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(
                apiCollection.getId());

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                String pathPattern = getPathPattern(
                    apiCollection.getContextPath(), apiCollection.getCollectionVersion(),
                    apiCollectionEndpoint.getPath());

                if (PATH_MATCHER.match(pathPattern, path) && apiCollectionEndpoint.getHttpMethod() == httpMethod) {
                    return new ApiCollectionEndpointResult(apiCollection, apiCollectionEndpoint);
                }
            }
        }

        throw new IllegalArgumentException("No API Collection endpoint found for request uri: " + path);
    }

    private Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader("x-environment");

        if (StringUtils.isNotBlank(environment)) {
            return Environment.valueOf(environment.toUpperCase());
        }

        return Environment.PRODUCTION;
    }

    private static String getPathPattern(
        String apiCollectionContextPath, int apiCollectionVersion, String apiCollectionEndpointPath) {

        return "/v" + apiCollectionVersion + "/" + apiCollectionContextPath +
            (apiCollectionEndpointPath == null ? "" : "/" + apiCollectionEndpointPath);
    }

    private record ApiCollectionEndpointResult(
        ApiCollection apiCollection, ApiCollectionEndpoint apiCollectionEndpoint) {
    }
}
