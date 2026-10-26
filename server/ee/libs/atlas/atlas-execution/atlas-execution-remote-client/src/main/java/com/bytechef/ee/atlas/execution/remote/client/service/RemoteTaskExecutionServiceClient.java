/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionResult;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskExecutionServiceClient implements TaskExecutionService {

    private static final String EXECUTION_APP = "execution-app";

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteTaskExecutionServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public boolean cancelIfUnfinished(long id) {
        BoolResponse boolResponse = getStub().cancelIfUnfinished(toIdRequest(id));

        return boolResponse.getValue();
    }

    @Override
    public boolean completeIfNotCancelled(long id) {
        BoolResponse boolResponse = getStub().completeIfNotCancelled(toIdRequest(id));

        return boolResponse.getValue();
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        return atlasExecutionProtoMapper
            .toTaskExecution(getStub().create(atlasExecutionProtoMapper.toProto(taskExecution)));
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJobTaskExecutions(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchLastCancelledLeafJobTaskExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchLastFailedJobTaskExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchLastJobTaskExecution(long jobId) {
        TaskExecutionResult taskExecutionResult = getStub().fetchLastJobTaskExecution(toIdRequest(jobId));

        if (!taskExecutionResult.hasTaskExecution()) {
            return Optional.empty();
        }

        return Optional.of(atlasExecutionProtoMapper.toTaskExecution(taskExecutionResult.getTaskExecution()));
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return atlasExecutionProtoMapper.toTaskExecutions(getStub().getJobTaskExecutions(toIdRequest(jobId)));
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return atlasExecutionProtoMapper.toTaskExecutions(getStub().getParentTaskExecutions(toIdRequest(parentId)));
    }

    @Override
    public TaskExecution getTaskExecution(long id) {
        return atlasExecutionProtoMapper.toTaskExecution(getStub().getTaskExecution(toIdRequest(id)));
    }

    @Override
    public TaskExecution getTaskExecutionForUpdate(long id) {
        return atlasExecutionProtoMapper.toTaskExecution(getStub().getTaskExecutionForUpdate(toIdRequest(id)));
    }

    @Override
    public long countJobTaskExecutions(long jobId, List<Long> supersededTaskExecutionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchLastStartedJobTaskExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TaskExecution> fetchTaskExecution(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Long> getChildCounts(List<Long> parentIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getChildIndex(TaskExecution taskExecution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getJobUnfinishedTaskExecutions(long jobId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TASK_EXECUTION_SERVICE + "/get-job-unfinished-task-executions/{jobId}")
                .build(jobId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TaskExecution> getJobTopLevelTaskExecutions(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIterationIndex(TaskExecution taskExecution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Long> getIterationCounts(List<Long> parentIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getLatestAttemptFailedLeafJobTaskExecutions(
        long jobId, List<Long> supersededTaskExecutionIds, int pageNumber, int pageSize) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getParentIterationStartTaskExecutions(long parentId, long offset, int limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getParentTaskExecutionIds(long parentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getParentTaskExecutions(long parentId, int pageNumber, int pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getParentTaskExecutionsBetween(
        long parentId, TaskExecution fromTaskExecution, TaskExecution toTaskExecution) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getParentTaskExecutionsFrom(long parentId, TaskExecution fromTaskExecution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TaskExecution> getTaskExecutions(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        return atlasExecutionProtoMapper
            .toTaskExecution(getStub().update(atlasExecutionProtoMapper.toProto(taskExecution)));
    }

    private TaskExecutionRemoteServiceGrpc.TaskExecutionRemoteServiceBlockingStub getStub() {
        return TaskExecutionRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }

    @Override
    public TaskExecution update(long id, Consumer<TaskExecution> taskExecutionModifier) {
        throw new UnsupportedOperationException();
    }
}
