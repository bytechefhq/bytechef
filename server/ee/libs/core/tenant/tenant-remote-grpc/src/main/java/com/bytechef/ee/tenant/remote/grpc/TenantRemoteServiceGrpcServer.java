/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.remote.grpc;

import com.bytechef.ee.tenant.remote.grpc.proto.TenantIdsResponse;
import com.bytechef.ee.tenant.remote.grpc.proto.TenantRemoteServiceGrpc;
import com.bytechef.tenant.service.TenantService;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TenantRemoteServiceGrpcServer extends TenantRemoteServiceGrpc.TenantRemoteServiceImplBase {

    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public TenantRemoteServiceGrpcServer(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void getTenantIds(Empty request, StreamObserver<TenantIdsResponse> responseObserver) {
        responseObserver.onNext(
            TenantIdsResponse.newBuilder()
                .addAllTenantIds(tenantService.getTenantIds())
                .build());
        responseObserver.onCompleted();
    }
}
