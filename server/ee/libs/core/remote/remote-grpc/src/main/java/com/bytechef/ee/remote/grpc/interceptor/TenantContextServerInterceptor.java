/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import com.bytechef.tenant.TenantContext;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.apache.commons.lang3.StringUtils;

/**
 * Binds the caller's tenant while a unary call runs on the server, and resets it afterwards.
 *
 * @version ee
 */
public class TenantContextServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        return new TenantBindingListener<>(
            serverCallHandler.startCall(serverCall, metadata), metadata.get(GrpcMetadataKeys.TENANT_ID));
    }

    private static final class TenantBindingListener<ReqT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final String tenantId;

        private TenantBindingListener(ServerCall.Listener<ReqT> delegate, String tenantId) {
            super(delegate);

            this.tenantId = tenantId;
        }

        @Override
        public void onHalfClose() {
            if (StringUtils.isBlank(tenantId)) {
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
