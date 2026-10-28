/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogRetentionJob;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobModel;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.grpc.BindableService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
public class ExecutionApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private Environment environment;

    @Autowired
    private MemoryMessageBroker memoryMessageBroker;

    @Autowired
    private ObjectProvider<ToolInvocationLogRetentionJob> toolInvocationLogRetentionJobProvider;

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix"))
            .isEqualTo("execution-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testDataStorageGrpcServiceIsServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains("bytechef.platform.data.storage.remote.v1.JdbcDataStorageRemoteService");
    }

    @Test
    void testExecutionGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.atlas.execution.remote.v1.ContextRemoteService",
            "bytechef.atlas.execution.remote.v1.CounterRemoteService",
            "bytechef.atlas.execution.remote.v1.JobFacadeRemoteService",
            "bytechef.atlas.execution.remote.v1.JobRemoteService",
            "bytechef.atlas.execution.remote.v1.TaskExecutionRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.ConnectionLifecycleFacadeRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.JobResumeFacadeRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.PrincipalJobFacadeRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.PrincipalJobRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.TaskStateRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.TriggerExecutionRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.TriggerLifecycleFacadeRemoteService",
            "bytechef.platform.workflow.execution.remote.v1.TriggerStateRemoteService");
        assertThat(serviceNames).doesNotContain(
            "bytechef.platform.configuration.remote.v1.WorkflowRemoteService",
            "bytechef.automation.configuration.remote.v1.ProjectRemoteService",
            "bytechef.platform.connection.remote.v1.ConnectionRemoteService",
            "bytechef.platform.component.remote.v1.ComponentDefinitionRemoteService",
            "bytechef.platform.workflow.task.dispatcher.remote.v1.TaskDispatcherDefinitionRemoteService",
            "bytechef.platform.scheduler.remote.v1.TriggerSchedulerRemoteService");
    }

    @Test
    void testJobConvertsToTheJobRestModels() {
        Job job = new Job(7L);

        job.setStatus(Job.Status.COMPLETED);
        job.setWorkflowId("workflow-id");

        JobModel jobModel = conversionService.convert(job, JobModel.class);

        assertThat(jobModel).isNotNull();
        assertThat(jobModel.getId()).isEqualTo("7");
        assertThat(jobModel.getStatus()).isEqualTo(JobModel.StatusEnum.COMPLETED);
        assertThat(jobModel.getWorkflowId()).isEqualTo("workflow-id");

        JobBasicModel jobBasicModel = conversionService.convert(job, JobBasicModel.class);

        assertThat(jobBasicModel).isNotNull();
        assertThat(jobBasicModel.getId()).isEqualTo("7");
        assertThat(jobBasicModel.getStatus()).isEqualTo(JobBasicModel.StatusEnum.COMPLETED);
        assertThat(jobBasicModel.getWorkflowId()).isEqualTo("workflow-id");
    }

    @Test
    void testJobEventsPublishedHereReachTheMessageBroker() throws Exception {
        CompletableFuture<Object> receivedMessage = new CompletableFuture<>();

        memoryMessageBroker.receive(TaskCoordinatorMessageRoute.JOB_START_EVENTS, receivedMessage::complete);

        applicationEventPublisher.publishEvent(new StartJobEvent(42L));

        Object message = receivedMessage.get(10, TimeUnit.SECONDS);

        assertThat(message).isInstanceOfSatisfying(
            StartJobEvent.class, startJobEvent -> assertThat(startJobEvent.getJobId()).isEqualTo(42L));
    }

    @Test
    void testIsTenantAdminResolvesInMethodSecurityExpressions() throws Throwable {
        MethodSecurityExpressionHandler methodSecurityExpressionHandler =
            applicationContext.getBean(MethodSecurityExpressionHandler.class);

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of());
        MethodInvocation methodInvocation =
            new SimpleMethodInvocation(new Object(), Object.class.getMethod("toString"));

        EvaluationContext evaluationContext = methodSecurityExpressionHandler.createEvaluationContext(
            () -> authentication, methodInvocation);
        Expression expression = methodSecurityExpressionHandler.getExpressionParser()
            .parseExpression("isTenantAdmin()");

        Boolean tenantAdmin = AutomationAuthorizationContext.callSkippingChecks(
            () -> expression.getValue(evaluationContext, Boolean.class));

        assertThat(tenantAdmin).isTrue();
    }

    @Test
    void testToolInvocationLogRetentionJobPurgesTheExecutionDatabase() {
        ToolInvocationLogRetentionJob toolInvocationLogRetentionJob =
            toolInvocationLogRetentionJobProvider.getIfAvailable();

        assertThat(toolInvocationLogRetentionJob).isNotNull();

        assertThatCode(toolInvocationLogRetentionJob::purgeExpiredLogs).doesNotThrowAnyException();
    }
}
