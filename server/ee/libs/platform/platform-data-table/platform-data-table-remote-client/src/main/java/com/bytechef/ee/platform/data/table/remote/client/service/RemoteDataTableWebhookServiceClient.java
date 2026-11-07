/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RemoveWebhookRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableWebhookServiceClient extends AbstractRemoteDataTableClient
    implements DataTableWebhookService {

    public RemoteDataTableWebhookServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public long addWebhook(
        String baseName, String workflowExecutionId, String url, DataTableWebhookType type, long environmentId) {

        AddWebhookResponse addWebhookResponse = call(
            () -> getStub().addWebhook(
                AddWebhookRequest.newBuilder()
                    .setBaseName(baseName)
                    .setWorkflowExecutionId(workflowExecutionId)
                    .setUrl(url)
                    .setType(type.name())
                    .setEnvironmentId(environmentId)
                    .build()));

        return addWebhookResponse.getId();
    }

    @Override
    public List<Webhook> listWebhooks(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWebhook(long id) {
        call(
            () -> getStub().removeWebhook(
                RemoveWebhookRequest.newBuilder()
                    .setId(id)
                    .build()));
    }
}
