/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public API Platform path: searches every collection of the environment named by the {@code x-environment} header,
 * PRODUCTION when absent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping(ApiPlatformHandlerController.API_PLATFORM_BASE_PATH + "/**")
@CrossOrigin
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ApiPlatformHandlerController extends AbstractApiPlatformEndpointController {

    protected static final String API_PLATFORM_BASE_PATH = "/api/o";

    private final ApiCollectionService apiCollectionService;

    @SuppressFBWarnings("EI")
    public ApiPlatformHandlerController(
        ApiCollectionService apiCollectionService, ApiCollectionEndpointService apiCollectionEndpointService,
        ApplicationProperties applicationProperties, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService) {

        super(
            apiCollectionEndpointService, applicationProperties, projectDeploymentService,
            projectDeploymentWorkflowService, projectWorkflowService, tempFileStorage, webhookWorkflowExecutor,
            workflowService);

        this.apiCollectionService = apiCollectionService;
    }

    @Override
    protected String getBasePath() {
        return API_PLATFORM_BASE_PATH;
    }

    @Override
    protected Environment getEnvironment(HttpServletRequest httpServletRequest) {
        String environment = httpServletRequest.getHeader("x-environment");

        if (StringUtils.isNotBlank(environment)) {
            return Environment.valueOf(environment.toUpperCase());
        }

        return Environment.PRODUCTION;
    }

    @Override
    protected List<ApiCollection> getSearchableApiCollections(Environment environment) {
        return apiCollectionService.getApiCollections(null, environment, null, null);
    }
}
