/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.remote.grpc.proto.PublicKeyResponse;
import com.bytechef.ee.embedded.security.remote.grpc.proto.SigningKeyPublicKeyRequest;
import com.bytechef.ee.embedded.security.remote.grpc.proto.SigningKeyRemoteServiceGrpc;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.security.PublicKey;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class RemoteSigningKeyServiceClient implements SigningKeyService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteSigningKeyServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public String create(SigningKey signingKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PublicKey getPublicKey(String keyId, long environmentId) {
        PublicKeyResponse publicKeyResponse;

        try {
            publicKeyResponse = getStub().getPublicKey(
                SigningKeyPublicKeyRequest.newBuilder()
                    .setEnvironmentId(environmentId)
                    .setKeyId(keyId)
                    .build());
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(status.getDescription(), statusRuntimeException);
            }

            throw statusRuntimeException;
        }

        return PublicKeyDecoder.decode(publicKeyResponse.getEncoded());
    }

    @Override
    public SigningKey getSigningKey(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SigningKey> getSigningKeys(PlatformType type, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SigningKey update(SigningKey signingKey) {
        throw new UnsupportedOperationException();
    }

    private SigningKeyRemoteServiceGrpc.SigningKeyRemoteServiceBlockingStub getStub() {
        return SigningKeyRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}
