/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteFileEntry;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJob;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJobParameters;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecution;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecutions;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps jobs, job parameters, file entries and task executions to their proto messages and back through
 * {@link ProtoJsonMapper}.
 *
 * @version ee
 */
public class AtlasExecutionProtoMapper {

    private final ProtoJsonMapper protoJsonMapper;
    private final TaskExecutionProtoMapper taskExecutionProtoMapper;

    public AtlasExecutionProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(Job.class, NoArgsCreatorMixIn.class)
                .addMixIn(JobParametersDTO.class, NoArgsCreatorMixIn.class)
                .build());
        this.taskExecutionProtoMapper = new TaskExecutionProtoMapper(objectMapper);
    }

    public FileEntry toFileEntry(RemoteFileEntry remoteFileEntry) {
        return protoJsonMapper.toObject(remoteFileEntry, FileEntry.class);
    }

    public Job toJob(RemoteJob remoteJob) {
        return protoJsonMapper.toObject(remoteJob, Job.class);
    }

    public JobParametersDTO toJobParametersDTO(RemoteJobParameters remoteJobParameters) {
        return protoJsonMapper.toObject(remoteJobParameters, JobParametersDTO.class);
    }

    public RemoteFileEntry toProto(FileEntry fileEntry) {
        return protoJsonMapper.toProto(fileEntry, RemoteFileEntry.newBuilder());
    }

    public RemoteJob toProto(Job job) {
        return protoJsonMapper.toProto(job, RemoteJob.newBuilder());
    }

    public RemoteJobParameters toProto(JobParametersDTO jobParametersDTO) {
        return protoJsonMapper.toProto(jobParametersDTO, RemoteJobParameters.newBuilder());
    }

    public RemoteTaskExecution toProto(TaskExecution taskExecution) {
        return taskExecutionProtoMapper.toProto(taskExecution);
    }

    public RemoteTaskExecutions toRemoteTaskExecutions(List<TaskExecution> taskExecutions) {
        RemoteTaskExecutions.Builder builder = RemoteTaskExecutions.newBuilder();

        for (TaskExecution taskExecution : taskExecutions) {
            builder.addTaskExecutions(toProto(taskExecution));
        }

        return builder.build();
    }

    public TaskExecution toTaskExecution(RemoteTaskExecution remoteTaskExecution) {
        return taskExecutionProtoMapper.toTaskExecution(remoteTaskExecution);
    }

    public List<TaskExecution> toTaskExecutions(RemoteTaskExecutions remoteTaskExecutions) {
        return remoteTaskExecutions.getTaskExecutionsList()
            .stream()
            .map(this::toTaskExecution)
            .toList();
    }

    private abstract static class NoArgsCreatorMixIn {

        @JsonCreator
        NoArgsCreatorMixIn() {
        }
    }
}
