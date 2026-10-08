/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mockito.Answers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = EnvironmentService.class, answers = Answers.CALLS_REAL_METHODS)
@MockitoBean(types = {
    ConnectedUserService.class, ConnectedUserWorkflowReferenceFacade.class,
    IntegrationInstanceConfigurationWorkflowService.class, IntegrationInstanceService.class,
    IntegrationInstanceWorkflowService.class, IntegrationWorkflowService.class, ProjectDeploymentService.class,
    ProjectWorkflowService.class, TempFileStorage.class, WebhookWorkflowExecutor.class, WorkflowService.class
})
public @interface EmbeddedWebhookPublicRestSharedMocks {
}
