/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.remote.client.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionSource;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.WorkflowExecutionProtoMapper;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetTriggerExecutionWorkflowExecutionRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetWorkflowExecutionRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.ProjectWorkflowExecutionRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionScopeDTO;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectWorkflowExecutionFacadeClient implements ProjectWorkflowExecutionFacade {

    public static final String SERVICE_ID = "execution-app";

    private final GrpcChannels grpcChannels;
    private final WorkflowExecutionProtoMapper workflowExecutionProtoMapper;

    public RemoteProjectWorkflowExecutionFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.workflowExecutionProtoMapper = new WorkflowExecutionProtoMapper(objectMapper);
    }

    @Override
    public WorkflowExecutionDTO getWorkflowExecution(long id, boolean lazy) {
        return workflowExecutionProtoMapper.toWorkflowExecutionDTO(
            getStub().getWorkflowExecution(
                GetWorkflowExecutionRequest.newBuilder()
                    .setId(id)
                    .setLazy(lazy)
                    .build()));
    }

    @Override
    public WorkflowExecutionDTO getTriggerExecutionWorkflowExecution(long triggerExecutionId) {
        return workflowExecutionProtoMapper.toWorkflowExecutionDTO(
            getStub().getTriggerExecutionWorkflowExecution(
                GetTriggerExecutionWorkflowExecutionRequest.newBuilder()
                    .setTriggerExecutionId(triggerExecutionId)
                    .build()));
    }

    @Override
    public WorkflowExecutionScopeDTO getEmbeddedWorkflowExecutionScope(
        Long environmentId, Status jobStatus, Long connectedUserId, long workspaceId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecutionDTO getWorkflowExecutionTaskExecution(long id, long taskExecutionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecutionChildrenPageDTO getWorkflowExecutionTaskExecutionChildren(
        long id, long taskExecutionId, int pageNumber, int pageSize) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Boolean embedded, Long environmentId, WorkflowExecutionSource workflowExecutionSource, Status jobStatus,
        Instant jobStartDate, Instant jobEndDate, Long projectId, Long projectDeploymentId, Long connectedUserId,
        String workflowId, Integer projectVersion, long workspaceId, int pageNumber) {

        return workflowExecutionProtoMapper.toPage(
            getStub().getWorkflowExecutions(
                workflowExecutionProtoMapper.toProto(
                    embedded, environmentId, workflowExecutionSource, jobStatus, jobStartDate, jobEndDate, projectId,
                    projectDeploymentId,
                    connectedUserId, workflowId, projectVersion, workspaceId, pageNumber)));
    }

    @Override
    public List<WorkflowExecutionDTO> getWorkflowExecutions(List<WorkflowExecutionRowDTO> workflowExecutionRowDTOs) {
        throw new UnsupportedOperationException();
    }

    private ProjectWorkflowExecutionRemoteServiceGrpc.ProjectWorkflowExecutionRemoteServiceBlockingStub getStub() {
        return ProjectWorkflowExecutionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}
