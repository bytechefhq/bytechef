/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.workflow.execution.remote.client.facade.RemoteConnectionLifecycleFacadeClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.facade.RemoteJobResumeFacadeClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.facade.RemotePrincipalJobFacadeClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.facade.RemoteTriggerLifecycleFacadeClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.service.RemotePrincipalJobServiceClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.service.RemoteTriggerExecutionServiceClient;
import com.bytechef.ee.platform.workflow.execution.remote.client.service.RemoteTriggerStateServiceClient;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.ConnectionLifecycleFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.JobResumeFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PrincipalJobFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PrincipalJobRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerExecutionRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerLifecycleFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.TriggerStateRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PlatformWorkflowExecutionRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final ConnectionLifecycleFacade connectionLifecycleFacade = mock(ConnectionLifecycleFacade.class);
    private final JobResumeFacade jobResumeFacade = mock(JobResumeFacade.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final TriggerLifecycleFacade triggerLifecycleFacade = mock(TriggerLifecycleFacade.class);
    private final TriggerStateService triggerStateService = mock(TriggerStateService.class);

    private ApplicationProperties applicationProperties;
    private FixedGrpcChannels fixedGrpcChannels;
    private ManagedChannel managedChannel;
    private Server server;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper =
            new PlatformWorkflowExecutionProtoMapper(JSON_MAPPER);
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(name)
            .directExecutor();

        for (BindableService bindableService : List.<BindableService>of(
            new ConnectionLifecycleFacadeRemoteServiceGrpcServer(
                connectionLifecycleFacade, platformWorkflowExecutionProtoMapper),
            new JobResumeFacadeRemoteServiceGrpcServer(jobResumeFacade, platformWorkflowExecutionProtoMapper),
            new PrincipalJobFacadeRemoteServiceGrpcServer(principalJobFacade, platformWorkflowExecutionProtoMapper),
            new PrincipalJobRemoteServiceGrpcServer(principalJobService),
            new TriggerExecutionRemoteServiceGrpcServer(
                triggerExecutionService, platformWorkflowExecutionProtoMapper),
            new TriggerLifecycleFacadeRemoteServiceGrpcServer(
                triggerLifecycleFacade, platformWorkflowExecutionProtoMapper),
            new TriggerStateRemoteServiceGrpcServer(triggerStateService, platformWorkflowExecutionProtoMapper))) {

            inProcessServerBuilder.addService(ServerInterceptors.intercept(bindableService, serverInterceptor));
        }

        server = inProcessServerBuilder.build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();
        fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        applicationProperties = new ApplicationProperties();

        applicationProperties.getRemote()
            .getGrpc()
            .setExecutionDeadline(Duration.ofSeconds(5));

        workflowExecutionId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1");
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testTriggerEnableWithoutConnection() {
        AtomicReference<String> receivedWorkflowExecutionId = new AtomicReference<>();

        doAnswer(invocation -> {
            receivedWorkflowExecutionId.set(String.valueOf(invocation.getArgument(1, WorkflowExecutionId.class)));

            return null;
        })
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq("workflow-1"), any(WorkflowExecutionId.class),
                eq(new WorkflowNodeType("webhook", 1, "awaitWorkflowAndRespond")), eq(Map.of("count", 5)), isNull(),
                eq("https://example.com/webhooks/abc"), eq(0L));

        triggerLifecycleFacadeClient().executeTriggerEnable(
            "workflow-1", workflowExecutionId, new WorkflowNodeType("webhook", 1, "awaitWorkflowAndRespond"),
            Map.of("count", 5), null, "https://example.com/webhooks/abc", 0L);

        assertThat(receivedWorkflowExecutionId.get()).isEqualTo(workflowExecutionId.toString());
    }

    @Test
    void testTriggerDisableCallsDisable() {
        triggerLifecycleFacadeClient().executeTriggerDisable(
            "workflow-1", workflowExecutionId, new WorkflowNodeType("webhook", 1, null), Map.of(), 1050L);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), any(WorkflowExecutionId.class), eq(new WorkflowNodeType("webhook", 1, null)),
            eq(Map.of()), eq(1050L));
        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            anyString(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testConnectionLifecycleCalls() {
        RemoteConnectionLifecycleFacadeClient remoteConnectionLifecycleFacadeClient =
            new RemoteConnectionLifecycleFacadeClient(fixedGrpcChannels, JSON_MAPPER);

        remoteConnectionLifecycleFacadeClient.scheduleConnectionRefresh(1050L, Map.of("token", "abc"), "public");
        remoteConnectionLifecycleFacadeClient.deleteScheduledConnectionRefresh(1050L, "public");

        verify(connectionLifecycleFacade).scheduleConnectionRefresh(1050L, Map.of("token", "abc"), "public");
        verify(connectionLifecycleFacade).deleteScheduledConnectionRefresh(1050L, "public");
    }

    @Test
    void testResumeJobReturnsTheOutcome() {
        when(jobResumeFacade.resumeExpiredJob("abc")).thenReturn(JobResumeOutcome.GONE);
        when(jobResumeFacade.resumeJob("def", Map.of("approved", true))).thenReturn(JobResumeOutcome.OK);

        RemoteJobResumeFacadeClient remoteJobResumeFacadeClient = new RemoteJobResumeFacadeClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteJobResumeFacadeClient.resumeExpiredJob("abc")).isEqualTo(JobResumeOutcome.GONE);
        assertThat(remoteJobResumeFacadeClient.resumeJob("def", Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.OK);
    }

    @Test
    void testCreateSyncJobReturnsTheJob() {
        Job job = new Job();

        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);
        job.setWorkflowId("workflow-1");

        when(principalJobFacade.createSyncJob(any(JobParametersDTO.class), eq(1051L), eq(PlatformType.AUTOMATION)))
            .thenReturn(job);
        when(principalJobFacade.createJob(any(JobParametersDTO.class), eq(1051L), eq(PlatformType.AUTOMATION)))
            .thenReturn(8L);
        when(principalJobFacade.createChildJob(eq(7L), any(JobParametersDTO.class), eq(PlatformType.EMBEDDED)))
            .thenReturn(9L);

        RemotePrincipalJobFacadeClient remotePrincipalJobFacadeClient = new RemotePrincipalJobFacadeClient(
            applicationProperties, fixedGrpcChannels, JSON_MAPPER);
        JobParametersDTO jobParametersDTO = new JobParametersDTO("workflow-1", Map.of("id", 9_007_199_254_740_993L));

        assertThat(remotePrincipalJobFacadeClient.createSyncJob(jobParametersDTO, 1051L, PlatformType.AUTOMATION))
            .usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remotePrincipalJobFacadeClient.createJob(jobParametersDTO, 1051L, PlatformType.AUTOMATION))
            .isEqualTo(8L);
        assertThat(remotePrincipalJobFacadeClient.createChildJob(7L, jobParametersDTO, PlatformType.EMBEDDED))
            .isEqualTo(9L);
    }

    @Test
    void testPrincipalJobLookups() {
        when(principalJobService.fetchLastJobId(1051L, PlatformType.AUTOMATION)).thenReturn(Optional.of(7L));
        when(principalJobService.fetchJobPrincipalId(7L, PlatformType.AUTOMATION)).thenReturn(Optional.of(1051L));
        when(principalJobService.fetchLastWorkflowJobId(1051L, List.of("a", "b"), PlatformType.AUTOMATION))
            .thenReturn(Optional.of(8L));

        RemotePrincipalJobServiceClient remotePrincipalJobServiceClient = new RemotePrincipalJobServiceClient(
            fixedGrpcChannels);

        assertThat(remotePrincipalJobServiceClient.fetchLastJobId(1051L, PlatformType.AUTOMATION)).contains(7L);
        assertThat(remotePrincipalJobServiceClient.fetchJobPrincipalId(7L, PlatformType.AUTOMATION)).contains(1051L);
        assertThat(remotePrincipalJobServiceClient.fetchLastWorkflowJobId(
            1051L, List.of("a", "b"), PlatformType.AUTOMATION)).contains(8L);
    }

    @Test
    void testFetchLastWorkflowJobIdWithoutJobIsEmpty() {
        when(principalJobService.fetchLastWorkflowJobId(1051L, List.of("a"), PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        assertThat(new RemotePrincipalJobServiceClient(fixedGrpcChannels).fetchLastWorkflowJobId(
            1051L, List.of("a"), PlatformType.AUTOMATION)).isEmpty();
    }

    @Test
    void testGetJobPrincipalIdReturnsThePrincipal() {
        when(principalJobService.fetchJobPrincipalId(7L, PlatformType.AUTOMATION)).thenReturn(Optional.of(1051L));

        assertThat(new RemotePrincipalJobServiceClient(fixedGrpcChannels).getJobPrincipalId(
            7L, PlatformType.AUTOMATION)).isEqualTo(1051L);
    }

    @Test
    void testGetJobPrincipalIdWithoutPrincipalThrows() {
        when(principalJobService.fetchJobPrincipalId(7L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());

        RemotePrincipalJobServiceClient remotePrincipalJobServiceClient = new RemotePrincipalJobServiceClient(
            fixedGrpcChannels);

        assertThatThrownBy(() -> remotePrincipalJobServiceClient.getJobPrincipalId(7L, PlatformType.AUTOMATION))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testTriggerExecutionCalls() {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(5L)
            .status(TriggerExecution.Status.STARTED)
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(new WorkflowTrigger(Map.of("name", "trigger_1", "type", "webhook/v1/a")))
            .build();

        when(triggerExecutionService.create(any(TriggerExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(triggerExecutionService.getTriggerExecution(5L)).thenReturn(triggerExecution);
        when(triggerExecutionService.update(any(TriggerExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        RemoteTriggerExecutionServiceClient remoteTriggerExecutionServiceClient =
            new RemoteTriggerExecutionServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteTriggerExecutionServiceClient.create(triggerExecution)
            .getId()).isEqualTo(5L);
        assertThat(String.valueOf(remoteTriggerExecutionServiceClient.getTriggerExecution(5L)
            .getWorkflowExecutionId())).isEqualTo(workflowExecutionId.toString());
        assertThat(remoteTriggerExecutionServiceClient.update(triggerExecution)
            .getStatus()).isEqualTo(TriggerExecution.Status.STARTED);
    }

    @Test
    void testTriggerStateKeepsNumberTypes() {
        when(triggerStateService.fetchValue(any(WorkflowExecutionId.class)))
            .thenReturn(Optional.of(9_007_199_254_740_993L));

        RemoteTriggerStateServiceClient remoteTriggerStateServiceClient = new RemoteTriggerStateServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteTriggerStateServiceClient.<Object>fetchValue(workflowExecutionId))
            .containsInstanceOf(Long.class)
            .contains(9_007_199_254_740_993L);

        when(triggerStateService.fetchValue(any(WorkflowExecutionId.class))).thenReturn(Optional.empty());

        assertThat(remoteTriggerStateServiceClient.<Object>fetchValue(workflowExecutionId)).isEmpty();

        remoteTriggerStateServiceClient.save(workflowExecutionId, Map.of("cursor", 5));
        remoteTriggerStateServiceClient.delete(workflowExecutionId);

        verify(triggerStateService).save(any(WorkflowExecutionId.class), eq(Map.of("cursor", 5)));
        verify(triggerStateService).delete(any(WorkflowExecutionId.class));
    }

    private RemoteTriggerLifecycleFacadeClient triggerLifecycleFacadeClient() {
        return new RemoteTriggerLifecycleFacadeClient(applicationProperties, fixedGrpcChannels, JSON_MAPPER);
    }
}
