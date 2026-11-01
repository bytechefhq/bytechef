/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetTriggerExecutionWorkflowExecutionRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetWorkflowExecutionRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.GetWorkflowExecutionsRequest;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.ProjectWorkflowExecutionRemoteServiceGrpc;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.RemoteWorkflowExecution;
import com.bytechef.ee.automation.workflow.execution.remote.grpc.proto.RemoteWorkflowExecutionPage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectWorkflowExecutionRemoteServiceGrpcServer
    extends ProjectWorkflowExecutionRemoteServiceGrpc.ProjectWorkflowExecutionRemoteServiceImplBase {

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;
    private final WorkflowExecutionProtoMapper workflowExecutionProtoMapper;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowExecutionRemoteServiceGrpcServer(
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade,
        WorkflowExecutionProtoMapper workflowExecutionProtoMapper) {

        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
        this.workflowExecutionProtoMapper = workflowExecutionProtoMapper;
    }

    @Override
    public void getTriggerExecutionWorkflowExecution(
        GetTriggerExecutionWorkflowExecutionRequest request,
        StreamObserver<RemoteWorkflowExecution> responseObserver) {

        WorkflowExecutionDTO workflowExecutionDTO = projectWorkflowExecutionFacade
            .getTriggerExecutionWorkflowExecution(request.getTriggerExecutionId());

        responseObserver.onNext(workflowExecutionProtoMapper.toProto(workflowExecutionDTO));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowExecution(
        GetWorkflowExecutionRequest request, StreamObserver<RemoteWorkflowExecution> responseObserver) {

        WorkflowExecutionDTO workflowExecutionDTO = projectWorkflowExecutionFacade.getWorkflowExecution(
            request.getId(), request.getLazy());

        responseObserver.onNext(workflowExecutionProtoMapper.toProto(workflowExecutionDTO));
        responseObserver.onCompleted();
    }

    @Override
    public void getWorkflowExecutions(
        GetWorkflowExecutionsRequest request, StreamObserver<RemoteWorkflowExecutionPage> responseObserver) {

        Page<WorkflowExecutionDTO> page = projectWorkflowExecutionFacade.getWorkflowExecutions(
            request.hasEmbedded() ? request.getEmbedded() : null,
            request.hasEnvironmentId() ? request.getEnvironmentId() : null,
            request.hasJobStatus() ? Job.Status.valueOf(request.getJobStatus()) : null,
            workflowExecutionProtoMapper.toInstant(request.hasJobStartDate(), request.getJobStartDate()),
            workflowExecutionProtoMapper.toInstant(request.hasJobEndDate(), request.getJobEndDate()),
            request.hasProjectId() ? request.getProjectId() : null,
            request.hasProjectDeploymentId() ? request.getProjectDeploymentId() : null,
            request.hasConnectedUserId() ? request.getConnectedUserId() : null,
            request.hasWorkflowId() ? request.getWorkflowId() : null,
            request.hasProjectVersion() ? request.getProjectVersion() : null, request.getWorkspaceId(),
            request.getPageNumber());

        responseObserver.onNext(workflowExecutionProtoMapper.toProto(page));
        responseObserver.onCompleted();
    }
}
