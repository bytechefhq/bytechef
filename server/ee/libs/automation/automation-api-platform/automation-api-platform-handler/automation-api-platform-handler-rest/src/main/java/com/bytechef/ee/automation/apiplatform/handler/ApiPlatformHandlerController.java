/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.tenant.TenantContext;
import com.bytechef.platform.tenant.util.TenantUtils;
import com.bytechef.platform.webhook.web.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@RequestMapping(ApiPlatformHandlerController.API_PLATFORM_BASE_PATH + "/**")
@ConditionalOnCoordinator
public class ApiPlatformHandlerController extends AbstractWebhookTriggerController {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    protected static final String API_PLATFORM_BASE_PATH = "/api/o";

    private final ApiCollectionService apiCollectionService;
    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiPlatformHandlerExecutor apiPlatformHandlerExecutor;

    @SuppressFBWarnings("EI")
    public ApiPlatformHandlerController(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ApiPlatformHandlerExecutor apiPlatformHandlerExecutor, FilesFileStorage filesFileStorage,
        InstanceAccessorRegistry instanceAccessorRegistry, TriggerDefinitionService triggerDefinitionService,
        WorkflowService workflowService) {

        super(filesFileStorage, instanceAccessorRegistry, triggerDefinitionService, null, workflowService);

        this.apiCollectionService = apiCollectionService;
        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiPlatformHandlerExecutor = apiPlatformHandlerExecutor;
    }

    @DeleteMapping(produces = "application/json")
    public Object handleDeleteMethod(final HttpServletRequest request) {
        return null;
    }

    @GetMapping(produces = "application/json")
    public Object handleGetMethod(final HttpServletRequest request) {
        return doHandle(request);
    }

    @PatchMapping(produces = "application/json")
    public Object handlePatchMethod(final HttpServletRequest request) {
        return null;
    }

    @PostMapping(produces = "application/json")
    public Object handlePostMethod(final HttpServletRequest request) {
        return null;
    }

    @PutMapping(produces = "application/json")
    public Object handlePutMethod(final HttpServletRequest request) {
        return null;
    }

    private Object doHandle(HttpServletRequest request) {
        return TenantUtils.callWithTenantId(TenantContext.getCurrentTenantId(), () -> {
            Map<String, List<String>> variables;

            String requestURI = request.getRequestURI();

            String path = requestURI.replace(API_PLATFORM_BASE_PATH, "");

            ApiCollectionEndpoint apiCollectionEndpoint = getApiCollectionEndpoint(path, getEnvironment(request));

            ApiCollection apiCollection = apiCollectionService.getApiCollection(
                apiCollectionEndpoint.getApiCollectionId());

            variables = PATH_MATCHER
                .extractUriTemplateVariables(
                    getPathPattern(apiCollection.getCollectionVersion(), apiCollectionEndpoint.getPath()), path)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

            // TODO fetch from New API Request Trigger
            WebhookTriggerFlags webhookTriggerFlags = new WebhookTriggerFlags(false, true, false, false);

            WebhookRequest webhookRequest = getWebhookRequest(request, webhookTriggerFlags);

            webhookRequest = new WebhookRequest(
                webhookRequest.headers(), MapUtils.concat(webhookRequest.parameters(), variables),
                webhookRequest.body(), webhookRequest.method());

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                ModeType.AUTOMATION, apiCollection.getProjectInstanceId(),
                apiCollectionEndpoint.getWorkflowReferenceCode(), "trigger_1");

            // TODO return response from ResponseToAPIRequest action

            return apiPlatformHandlerExecutor.execute(workflowExecutionId, webhookRequest);
        });
    }

    private ApiCollectionEndpoint getApiCollectionEndpoint(String path, Environment environment) {
        for (ApiCollection apiCollection : apiCollectionService.getApiCollections(null, environment, null, null)) {
            List<ApiCollectionEndpoint> apiCollectionEndpoints = apiCollectionEndpointService.getApiEndpoints(
                apiCollection.getId());

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                if (PATH_MATCHER.match(
                    getPathPattern(apiCollection.getCollectionVersion(), apiCollectionEndpoint.getPath()), path)) {

                    return apiCollectionEndpoint;
                }
            }
        }

        throw new IllegalArgumentException("No API Collection endpoint found for request uri: " + path);
    }

    protected Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader("x-environment");

        if (StringUtils.isNotBlank(environment)) {
            return Environment.valueOf(environment.toUpperCase());
        }

        return Environment.PRODUCTION;
    }

    private static String getPathPattern(int collectionVersion, String apiCollectionEndpointPath) {
        return "/v" + collectionVersion + "/" + apiCollectionEndpointPath;
    }
}
