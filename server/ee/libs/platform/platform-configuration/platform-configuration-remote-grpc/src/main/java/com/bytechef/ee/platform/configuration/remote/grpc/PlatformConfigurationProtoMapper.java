/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteComponentConnection;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteComponentConnections;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteWorkflow;
import com.bytechef.ee.platform.configuration.remote.grpc.proto.RemoteWorkflows;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps workflows and component connections to their proto messages and back. A workflow travels by its persistent
 * fields and is rebuilt from its definition, so its tasks, inputs and outputs are parsed as everywhere else.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class PlatformConfigurationProtoMapper {

    private final JsonBytes jsonBytes;

    public PlatformConfigurationProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public ByteString toBytes(@Nullable Object value) {
        return jsonBytes.write(value);
    }

    public ComponentConnection toComponentConnection(RemoteComponentConnection remoteComponentConnection) {
        return new ComponentConnection(
            remoteComponentConnection.getComponentName(), remoteComponentConnection.getComponentVersion(),
            remoteComponentConnection.getWorkflowNodeName(), remoteComponentConnection.getKey(),
            remoteComponentConnection.getRequired());
    }

    public List<ComponentConnection> toComponentConnections(RemoteComponentConnections remoteComponentConnections) {
        return remoteComponentConnections.getConnectionsList()
            .stream()
            .map(this::toComponentConnection)
            .toList();
    }

    public @Nullable Map<String, Object> toMap(ByteString bytes) {
        return jsonBytes.readMap(bytes);
    }

    public RemoteComponentConnection toProto(ComponentConnection componentConnection) {
        return RemoteComponentConnection.newBuilder()
            .setComponentName(componentConnection.componentName())
            .setComponentVersion(componentConnection.componentVersion())
            .setKey(componentConnection.key())
            .setRequired(componentConnection.required())
            .setWorkflowNodeName(componentConnection.workflowNodeName())
            .build();
    }

    public RemoteWorkflow toProto(Workflow workflow) {
        Workflow.Format format = workflow.getFormat();

        RemoteWorkflow.Builder builder = RemoteWorkflow.newBuilder()
            .setDefinition(workflow.getDefinition())
            .setFormat(format.name())
            .setMetadata(jsonBytes.write(workflow.getMetadata()))
            .setVersion(workflow.getVersion());

        String id = workflow.getId();

        if (id != null) {
            builder.setId(id);
        }

        Instant lastModifiedDate = workflow.getLastModifiedDate();

        if (lastModifiedDate != null) {
            builder.setLastModifiedDate(toTimestamp(lastModifiedDate));
        }

        Workflow.SourceType sourceType = workflow.getSourceType();

        if (sourceType != null) {
            builder.setSourceType(sourceType.name());
        }

        return builder.build();
    }

    public RemoteComponentConnections toRemoteComponentConnections(List<ComponentConnection> componentConnections) {
        RemoteComponentConnections.Builder builder = RemoteComponentConnections.newBuilder();

        for (ComponentConnection componentConnection : componentConnections) {
            builder.addConnections(toProto(componentConnection));
        }

        return builder.build();
    }

    public RemoteWorkflows toRemoteWorkflows(List<Workflow> workflows) {
        RemoteWorkflows.Builder builder = RemoteWorkflows.newBuilder();

        for (Workflow workflow : workflows) {
            builder.addWorkflows(toProto(workflow));
        }

        return builder.build();
    }

    public Workflow toWorkflow(RemoteWorkflow remoteWorkflow) {
        Workflow workflow = new Workflow(
            remoteWorkflow.hasId() ? remoteWorkflow.getId() : null, remoteWorkflow.getDefinition(),
            Workflow.Format.valueOf(remoteWorkflow.getFormat()),
            remoteWorkflow.hasLastModifiedDate() ? toInstant(remoteWorkflow.getLastModifiedDate()) : null,
            Objects.requireNonNullElse(jsonBytes.readMap(remoteWorkflow.getMetadata()), Map.of()));

        workflow.setNew(false);
        workflow.setVersion(remoteWorkflow.getVersion());

        if (remoteWorkflow.hasSourceType()) {
            workflow.setSourceType(Workflow.SourceType.valueOf(remoteWorkflow.getSourceType()));
        }

        return workflow;
    }

    public List<Workflow> toWorkflows(RemoteWorkflows remoteWorkflows) {
        return remoteWorkflows.getWorkflowsList()
            .stream()
            .map(this::toWorkflow)
            .toList();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
