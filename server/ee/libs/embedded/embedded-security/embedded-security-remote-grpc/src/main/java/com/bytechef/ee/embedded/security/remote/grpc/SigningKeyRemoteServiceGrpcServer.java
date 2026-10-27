/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.grpc;

import com.bytechef.ee.embedded.security.remote.grpc.proto.PublicKeyResponse;
import com.bytechef.ee.embedded.security.remote.grpc.proto.SigningKeyPublicKeyRequest;
import com.bytechef.ee.embedded.security.remote.grpc.proto.SigningKeyRemoteServiceGrpc;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.security.PublicKey;

/**
 * Serves signing key public keys to the microservices that verify connected user tokens. An unknown key id is answered
 * with {@code INVALID_ARGUMENT}.
 *
 * @version ee
 */
public class SigningKeyRemoteServiceGrpcServer extends SigningKeyRemoteServiceGrpc.SigningKeyRemoteServiceImplBase {

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public SigningKeyRemoteServiceGrpcServer(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    public void getPublicKey(SigningKeyPublicKeyRequest request, StreamObserver<PublicKeyResponse> responseObserver) {
        PublicKey publicKey;

        try {
            publicKey = signingKeyService.getPublicKey(request.getKeyId(), request.getEnvironmentId());
        } catch (IllegalArgumentException illegalArgumentException) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT.withDescription(illegalArgumentException.getMessage())
                    .asRuntimeException());

            return;
        }

        responseObserver.onNext(
            PublicKeyResponse.newBuilder()
                .setEncoded(ByteString.copyFrom(publicKey.getEncoded()))
                .build());
        responseObserver.onCompleted();
    }
}
