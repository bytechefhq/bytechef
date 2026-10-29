/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJob;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJobParameters;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobIdsPage;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.RemoteTriggerExecution;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.RemoteWorkflowNodeType;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TaskStateValue;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.TriggerStateValue;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps trigger executions, workflow node types, jobs and job parameters to their proto messages and back. A trigger
 * execution's workflow execution id travels as its string form and its workflow trigger as the JSON of its map.
 *
 * @version ee
 */
public class PlatformWorkflowExecutionProtoMapper {

    private static final List<String> ALLOWED_STATE_CLASS_PREFIXES = List.of("java.", "com.bytechef.");

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final JsonBytes jsonBytes;
    private final ObjectMapper objectMapper;
    private final ProtoJsonMapper protoJsonMapper;

    @SuppressFBWarnings("EI2")
    public PlatformWorkflowExecutionProtoMapper(ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.jsonBytes = new JsonBytes(objectMapper);
        this.objectMapper = objectMapper;
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(TriggerExecution.class, TriggerExecutionMixIn.class)
                .build());
    }

    public ByteString toBytes(@Nullable Object value) {
        return jsonBytes.write(value);
    }

    public Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public Page<Long> toJobIdsPage(JobIdsPage jobIdsPage) {
        if (!jobIdsPage.getPaged()) {
            return new PageImpl<>(jobIdsPage.getJobIdsList());
        }

        return new PageImpl<>(
            jobIdsPage.getJobIdsList(), PageRequest.of(jobIdsPage.getPageNumber(), jobIdsPage.getPageSize()),
            jobIdsPage.getTotalElements());
    }

    public Job toJob(RemoteJob remoteJob) {
        return atlasExecutionProtoMapper.toJob(remoteJob);
    }

    public JobParametersDTO toJobParametersDTO(RemoteJobParameters remoteJobParameters) {
        return atlasExecutionProtoMapper.toJobParametersDTO(remoteJobParameters);
    }

    public @Nullable Map<String, Object> toMap(ByteString bytes) {
        return jsonBytes.readMap(bytes);
    }

    public @Nullable Object toObject(ByteString bytes) {
        return jsonBytes.read(bytes);
    }

    public JobIdsPage toProto(Page<Long> jobIdsPage) {
        JobIdsPage.Builder builder = JobIdsPage.newBuilder()
            .addAllJobIds(jobIdsPage.getContent())
            .setTotalElements(jobIdsPage.getTotalElements());

        Pageable pageable = jobIdsPage.getPageable();

        if (pageable.isPaged()) {
            builder.setPaged(true)
                .setPageNumber(pageable.getPageNumber())
                .setPageSize(pageable.getPageSize());
        }

        return builder.build();
    }

    public RemoteJob toProto(Job job) {
        return atlasExecutionProtoMapper.toProto(job);
    }

    public RemoteJobParameters toProto(JobParametersDTO jobParametersDTO) {
        return atlasExecutionProtoMapper.toProto(jobParametersDTO);
    }

    public RemoteTriggerExecution toProto(TriggerExecution triggerExecution) {
        RemoteTriggerExecution remoteTriggerExecution = protoJsonMapper.toProto(
            triggerExecution, RemoteTriggerExecution.newBuilder());

        RemoteTriggerExecution.Builder builder = remoteTriggerExecution.toBuilder();

        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        if (workflowExecutionId != null) {
            builder.setWorkflowExecutionId(workflowExecutionId.toString());
        }

        WorkflowTrigger workflowTrigger = triggerExecution.getWorkflowTrigger();

        if (workflowTrigger != null) {
            builder.setWorkflowTrigger(jsonBytes.write(workflowTrigger.toMap()));
        }

        return builder.build();
    }

    public RemoteWorkflowNodeType toProto(WorkflowNodeType workflowNodeType) {
        RemoteWorkflowNodeType.Builder builder = RemoteWorkflowNodeType.newBuilder()
            .setName(workflowNodeType.name())
            .setVersion(workflowNodeType.version());

        String operation = workflowNodeType.operation();

        if (operation != null) {
            builder.setOperation(operation);
        }

        return builder.build();
    }

    public TaskStateValue toTaskStateValue(@Nullable Object value) {
        TaskStateValue.Builder builder = TaskStateValue.newBuilder();

        if (value != null) {
            Class<?> valueClass = value.getClass();

            builder.setClassname(valueClass.getName())
                .setValue(jsonBytes.write(value));
        }

        return builder.build();
    }

    public @Nullable Object toTaskStateObject(TaskStateValue taskStateValue) {
        if (!taskStateValue.hasClassname()) {
            return null;
        }

        return toStateObject(taskStateValue.getValue(), taskStateValue.getClassname(), "task");
    }

    public TriggerStateValue toTriggerStateValue(@Nullable Object value) {
        TriggerStateValue.Builder builder = TriggerStateValue.newBuilder();

        if (value != null) {
            Class<?> valueClass = value.getClass();

            builder.setClassname(valueClass.getName())
                .setValue(jsonBytes.write(value));
        }

        return builder.build();
    }

    public @Nullable Object toTriggerStateObject(TriggerStateValue triggerStateValue) {
        if (!triggerStateValue.hasClassname()) {
            return null;
        }

        return toStateObject(triggerStateValue.getValue(), triggerStateValue.getClassname(), "trigger");
    }

    public Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    public TriggerExecution toTriggerExecution(RemoteTriggerExecution remoteTriggerExecution) {
        TriggerExecution triggerExecution = protoJsonMapper.toObject(remoteTriggerExecution, TriggerExecution.class);

        if (remoteTriggerExecution.hasWorkflowExecutionId()) {
            triggerExecution.setWorkflowExecutionId(
                WorkflowExecutionId.parse(remoteTriggerExecution.getWorkflowExecutionId()));
        }

        Map<String, Object> workflowTriggerMap = jsonBytes.readMap(remoteTriggerExecution.getWorkflowTrigger());

        if (workflowTriggerMap != null) {
            triggerExecution.setWorkflowTrigger(new WorkflowTrigger(workflowTriggerMap));
        }

        return triggerExecution;
    }

    public WorkflowNodeType toWorkflowNodeType(RemoteWorkflowNodeType remoteWorkflowNodeType) {
        return new WorkflowNodeType(
            remoteWorkflowNodeType.getName(), remoteWorkflowNodeType.getVersion(),
            remoteWorkflowNodeType.hasOperation() ? remoteWorkflowNodeType.getOperation() : null);
    }

    private Object toStateObject(ByteString value, String classname, String stateName) {
        if (ALLOWED_STATE_CLASS_PREFIXES.stream()
            .noneMatch(classname::startsWith)) {

            throw new IllegalArgumentException("Disallowed class in " + stateName + " state: " + classname);
        }

        try {
            return objectMapper.convertValue(jsonBytes.read(value), Class.forName(classname));
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(
                "Unknown class in " + stateName + " state: " + classname, classNotFoundException);
        }
    }

    @JsonIgnoreProperties(
        value = {
            "retryDelayMillis", "workflowExecutionId", "workflowTrigger"
        })
    private abstract static class TriggerExecutionMixIn {
    }
}
