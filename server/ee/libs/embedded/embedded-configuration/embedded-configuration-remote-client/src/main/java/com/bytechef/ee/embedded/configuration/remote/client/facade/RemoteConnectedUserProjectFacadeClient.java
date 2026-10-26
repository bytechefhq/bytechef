/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.ConnectedUserProjectFacadeRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.CopyWorkflowTemplateRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.StringResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserProjectFacadeClient implements ConnectedUserProjectFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectedUserProjectFacadeClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public String copyWorkflowTemplate(String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProjectFacadeRemoteServiceGrpc.ConnectedUserProjectFacadeRemoteServiceBlockingStub stub =
            ConnectedUserProjectFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));

        StringResponse stringResponse = stub.copyWorkflowTemplate(
            CopyWorkflowTemplateRequest.newBuilder()
                .setEnvironment(environment.name())
                .setExternalUserId(externalUserId)
                .setWorkflowUuid(workflowUuid)
                .build());

        return stringResponse.getValue();
    }

    @Override
    public String createProjectWorkflow(String externalUserId, String definition, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConnectedUserProjects(long connectedUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createProjectWorkflow(
        String externalUserId, String prompt, @Nullable String systemPrompt, Environment environment,
        boolean generate) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String enableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, Long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProjectWorkflowDTO getConnectedUserProjectWorkflow(
        String externalUserId, String workflowUuid, Long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CopilotChatContextDTO prepareCopilotChat(
        String externalUserId, String workflowUuid, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void publishProjectWorkflow(
        String externalUserId, String workflowUuid, String description, Long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflow(
        String externalUserId, String workflowUuid, String definition, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String updateProjectWorkflow(
        String externalUserId, String workflowUuid, String prompt, @Nullable String systemPrompt,
        Environment environment, boolean generate) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflowInputs(
        String externalUserId, String workflowUuid, Map<String, ?> inputs, Long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflowConfigurationConnection(
        String externalUserId, String workflowUuid, String workflowNodeName, String workflowConnectionKey,
        long connectionId, Environment environment) {

        throw new UnsupportedOperationException();
    }
}
