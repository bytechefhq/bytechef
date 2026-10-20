/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.config;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateProxyClient;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
    ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class, ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EnvironmentService.class, GitHubProxyClient.class, JobFacade.class, JobService.class,
    PermissionService.class, PrincipalJobFacade.class, PrincipalJobService.class, SharedTemplateFileStorage.class,
    TaskExecutionService.class,
    TriggerDefinitionService.class, TriggerExecutionService.class, TriggerLifecycleFacade.class, UserService.class,
    WorkflowCacheManager.class, WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTemplateProxyClient.class, WorkflowTestConfigurationService.class, WorkspaceService.class
})
public @interface ApiCollectionIntTestConfigurationSharedMocks {
}
