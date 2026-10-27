/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import com.bytechef.ee.embedded.security.remote.grpc.proto.JwtPublicKeyRequest;
import com.bytechef.ee.embedded.security.remote.grpc.proto.JwtTokenRemoteServiceGrpc;
import com.bytechef.ee.embedded.security.remote.grpc.proto.PublicKeyResponse;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.security.PublicKey;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class RemoteJwtTokenServiceClient implements JwtTokenService {

    private static final String SERVICE_ID = "configuration-app";

    private final GrpcChannels grpcChannels;

    public RemoteJwtTokenServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public String generateJwtToken(String externalUserId, long integrationId, int environmentId, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PublicKey getPublicKey(String keyId) {
        PublicKeyResponse publicKeyResponse = getStub().getPublicKey(
            JwtPublicKeyRequest.newBuilder()
                .setKeyId(keyId)
                .build());

        if (!publicKeyResponse.hasEncoded()) {
            return null;
        }

        return PublicKeyDecoder.decode(publicKeyResponse.getEncoded());
    }

    private JwtTokenRemoteServiceGrpc.JwtTokenRemoteServiceBlockingStub getStub() {
        return JwtTokenRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }
}
