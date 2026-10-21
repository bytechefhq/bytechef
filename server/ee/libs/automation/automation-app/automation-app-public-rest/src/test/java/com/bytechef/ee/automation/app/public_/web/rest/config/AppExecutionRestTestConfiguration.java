/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.app.facade.AppApiExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.AppApiCollectionApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppApiEndpointController;
import com.bytechef.ee.automation.app.public_.web.rest.AppConnectionApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppProxyApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppToolApiController;
import com.bytechef.ee.automation.app.public_.web.rest.AppWorkflowApiController;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The {@code /api/app/v1} controllers an app's secret key reaches, with the global exception advice they share in
 * production, for {@code @WebMvcTest}s. Each test declares {@code @MockitoBean}s for {@code AppExecutionFacade} and
 * {@code AppWorkflowExecutionFacade}. The API Platform endpoint controller's dependencies are plain mocks here; a test
 * that stubs one declares it as a {@code @MockitoBean}, which replaces the mock below.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Import({
    AppApiCollectionApiController.class, AppApiEndpointController.class, AppConnectionApiController.class,
    AppProxyApiController.class, AppToolApiController.class, AppWorkflowApiController.class,
    GlobalResponseEntityExceptionHandler.class, JacksonConfiguration.class
})
public class AppExecutionRestTestConfiguration {

    @Bean
    ApiCollectionEndpointService apiCollectionEndpointService() {
        return mock(ApiCollectionEndpointService.class);
    }

    @Bean
    AppApiExecutionFacade appApiExecutionFacade() {
        return mock(AppApiExecutionFacade.class);
    }

    @Bean
    ApplicationProperties applicationProperties() {
        return mock(ApplicationProperties.class);
    }

    @Bean
    ProjectDeploymentService projectDeploymentService() {
        return mock(ProjectDeploymentService.class);
    }

    @Bean
    ProjectDeploymentWorkflowService projectDeploymentWorkflowService() {
        return mock(ProjectDeploymentWorkflowService.class);
    }

    @Bean
    ProjectWorkflowService projectWorkflowService() {
        return mock(ProjectWorkflowService.class);
    }

    @Bean
    TempFileStorage tempFileStorage() {
        return mock(TempFileStorage.class);
    }

    @Bean
    WebhookWorkflowExecutor webhookWorkflowExecutor() {
        return mock(WebhookWorkflowExecutor.class);
    }

    @Bean
    WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }
}
