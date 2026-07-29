/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.interceptor;

import com.bytechef.tenant.TenantContext;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * gRPC client interceptor that propagates the caller's current tenant id as call metadata, mirroring the
 * {@code CURRENT_TENANT_ID} header the REST client relies on for multi-tenant routing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TenantContextClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
            channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {
                String tenantId = TenantContext.getCurrentTenantId();

                if (tenantId != null && !tenantId.isBlank()) {
                    metadata.put(GrpcMetadataKeys.CURRENT_TENANT_ID, tenantId);
                }

                super.start(responseListener, metadata);
            }
        };
    }
}
