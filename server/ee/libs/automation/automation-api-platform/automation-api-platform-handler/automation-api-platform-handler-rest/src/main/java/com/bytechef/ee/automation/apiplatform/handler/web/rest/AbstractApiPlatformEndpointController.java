/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
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
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Resolves a request to an API collection endpoint and runs the endpoint's workflow through its
 * {@code apiPlatform/newApiRequest} trigger. Subclasses decide which collections are searched, how the environment is
 * chosen and what an unmatched or unavailable endpoint answers. Endpoints are matched against
 * {@code /v{collectionVersion}/{contextPath}/{endpointPath}} in collection order, then endpoint order; the first match
 * wins.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractApiPlatformEndpointController extends AbstractWebhookTriggerController {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String API_PLATFORM_TRIGGER_COMPONENT_NAME = "apiPlatform";
    private static final String API_PLATFORM_TRIGGER_OPERATION_NAME = "newApiRequest";

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    protected AbstractApiPlatformEndpointController(
        ApiCollectionEndpointService apiCollectionEndpointService, ApplicationProperties applicationProperties,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService) {

        super(applicationProperties.getPublicUrl(), tempFileStorage, webhookWorkflowExecutor);

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
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

    /**
     * The path prefix stripped from the request URI before matching, e.g. {@code /api/o}.
     */
    protected abstract String getBasePath();

    protected abstract Environment getEnvironment(HttpServletRequest httpServletRequest);

    /**
     * The collections searched for a matching endpoint, in match order.
     */
    protected abstract List<ApiCollection> getSearchableApiCollections(Environment environment);

    protected ResponseEntity<?> doHandle(
        HttpMethod httpMethod, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        return TenantContext.callWithTenantId(TenantContext.getCurrentTenantId(), () -> {
            String path = StringUtils.removeStart(httpServletRequest.getRequestURI(), getBasePath());

            Optional<ApiCollectionEndpointResult> apiCollectionEndpointResult = findApiCollectionEndpoint(
                httpMethod, path, getSearchableApiCollections(getEnvironment(httpServletRequest)));

            if (apiCollectionEndpointResult.isEmpty()) {
                return notFound(path);
            }

            ApiCollection apiCollection = apiCollectionEndpointResult.get()
                .apiCollection();
            ApiCollectionEndpoint apiCollectionEndpoint = apiCollectionEndpointResult.get()
                .apiCollectionEndpoint();

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                    apiCollectionEndpoint.getProjectDeploymentWorkflowId());

            if (!projectDeploymentWorkflow.isEnabled()) {
                return unavailable("API Collection Endpoint is not enabled");
            }

            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                apiCollection.getProjectDeploymentId());

            if (!projectDeployment.isEnabled()) {
                return unavailable("API Collection is not enabled");
            }

            Map<String, List<String>> variables = PATH_MATCHER
                .extractUriTemplateVariables(getPathPattern(apiCollection, apiCollectionEndpoint), path)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

            WebhookTriggerFlags webhookTriggerFlags = new WebhookTriggerFlags(false, true, false, false);

            WebhookRequest webhookRequest = getWebhookRequest(httpServletRequest, webhookTriggerFlags);

            WebhookRequest endpointWebhookRequest = new WebhookRequest(
                webhookRequest.headers(), MapUtils.concat(webhookRequest.parameters(), variables),
                webhookRequest.body(), webhookRequest.method());

            String workflowId = projectDeploymentWorkflow.getWorkflowId();

            String workflowUuid = projectWorkflowService.getProjectWorkflowUuid(
                projectDeployment.getId(), workflowId);

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, apiCollection.getProjectDeploymentId(), workflowUuid,
                getApiPlatformTriggerName(workflowId));

            return executeEndpoint(
                apiCollection, apiCollectionEndpoint,
                () -> processTrigger(
                    workflowExecutionId, endpointWebhookRequest, httpServletRequest, httpServletResponse));
        });
    }

    /**
     * Runs the matched endpoint's workflow. Subclasses may wrap the execution, e.g. to record it.
     */
    protected ResponseEntity<?> executeEndpoint(
        ApiCollection apiCollection, ApiCollectionEndpoint apiCollectionEndpoint,
        Supplier<ResponseEntity<?>> execution) {

        return execution.get();
    }

    /**
     * The answer when no endpoint of the searchable collections matches {@code path}.
     */
    protected ResponseEntity<?> notFound(String path) {
        return ResponseEntity.status(404)
            .body("No API Collection endpoint found for request uri: " + path);
    }

    /**
     * The answer when the matched endpoint's workflow or its collection's deployment is disabled.
     */
    protected ResponseEntity<?> unavailable(String message) {
        return ResponseEntity.status(404)
            .body(message);
    }

    protected static String getPathPattern(ApiCollection apiCollection, ApiCollectionEndpoint apiCollectionEndpoint) {
        String endpointPath = apiCollectionEndpoint.getPath();

        return getCollectionPathPrefix(apiCollection) + (endpointPath == null ? "" : "/" + endpointPath);
    }

    /**
     * The {@code /v{collectionVersion}/{contextPath}} prefix every endpoint path of the collection starts with.
     */
    protected static String getCollectionPathPrefix(ApiCollection apiCollection) {
        return "/v" + apiCollection.getCollectionVersion() + "/" + apiCollection.getContextPath();
    }

    private Optional<ApiCollectionEndpointResult> findApiCollectionEndpoint(
        HttpMethod httpMethod, String path, List<ApiCollection> apiCollections) {

        for (ApiCollection apiCollection : apiCollections) {
            List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(
                apiCollection.getId());

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                String pathPattern = getPathPattern(apiCollection, apiCollectionEndpoint);

                if (PATH_MATCHER.match(pathPattern, path) && apiCollectionEndpoint.getHttpMethod() == httpMethod) {
                    return Optional.of(new ApiCollectionEndpointResult(apiCollection, apiCollectionEndpoint));
                }
            }
        }

        return Optional.empty();
    }

    private String getApiPlatformTriggerName(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), API_PLATFORM_TRIGGER_COMPONENT_NAME) &&
                Objects.equals(workflowNodeType.operation(), API_PLATFORM_TRIGGER_OPERATION_NAME)) {

                return workflowTrigger.getName();
            }
        }

        throw new IllegalStateException(
            "No API Platform trigger found in workflow '%s'".formatted(workflowId));
    }

    private ResponseEntity<?> processTrigger(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        try {
            return doProcessTrigger(workflowExecutionId, webhookRequest, httpServletRequest, httpServletResponse);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        } catch (ServletException servletException) {
            throw new IllegalStateException(servletException);
        }
    }

    private record ApiCollectionEndpointResult(
        ApiCollection apiCollection, ApiCollectionEndpoint apiCollectionEndpoint) {
    }
}
