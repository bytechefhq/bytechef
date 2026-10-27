/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.grpc;

import com.bytechef.ee.embedded.security.remote.grpc.proto.JwtPublicKeyRequest;
import com.bytechef.ee.embedded.security.remote.grpc.proto.JwtTokenRemoteServiceGrpc;
import com.bytechef.ee.embedded.security.remote.grpc.proto.PublicKeyResponse;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.security.PublicKey;

/**
 * @version ee
 */
public class JwtTokenRemoteServiceGrpcServer extends JwtTokenRemoteServiceGrpc.JwtTokenRemoteServiceImplBase {

    private final JwtTokenService jwtTokenService;

    @SuppressFBWarnings("EI")
    public JwtTokenRemoteServiceGrpcServer(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void getPublicKey(JwtPublicKeyRequest request, StreamObserver<PublicKeyResponse> responseObserver) {
        PublicKey publicKey = jwtTokenService.getPublicKey(request.getKeyId());

        PublicKeyResponse.Builder builder = PublicKeyResponse.newBuilder();

        if (publicKey != null) {
            builder.setEncoded(ByteString.copyFrom(publicKey.getEncoded()));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
