/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.interceptor;

import com.bytechef.tenant.TenantContext;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.jspecify.annotations.Nullable;

/**
 * gRPC server interceptor mirroring {@code RemoteMultiTenantFilter}: reads the tenant id from call metadata and binds
 * it to the {@link TenantContext} thread-local around the unary handler invocation ({@code onHalfClose}), clearing it
 * afterwards so the tenant never leaks across calls on a pooled thread.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TenantContextServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        String tenantId = metadata.get(GrpcMetadataKeys.CURRENT_TENANT_ID);

        return new TenantBindingListener<>(serverCallHandler.startCall(serverCall, metadata), tenantId);
    }

    private static final class TenantBindingListener<ReqT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final @Nullable String tenantId;

        private TenantBindingListener(ServerCall.Listener<ReqT> delegate, @Nullable String tenantId) {
            super(delegate);

            this.tenantId = tenantId;
        }

        @Override
        public void onHalfClose() {
            if (tenantId == null || tenantId.isBlank()) {
                super.onHalfClose();

                return;
            }

            TenantContext.setCurrentTenantId(tenantId);

            try {
                super.onHalfClose();
            } finally {
                TenantContext.resetCurrentTenantId();
            }
        }
    }
}
