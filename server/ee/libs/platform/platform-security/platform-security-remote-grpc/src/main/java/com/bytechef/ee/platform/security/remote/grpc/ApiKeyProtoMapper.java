/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.grpc;

import com.bytechef.ee.platform.security.remote.grpc.proto.RemoteApiKey;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.google.protobuf.Timestamp;
import java.time.Instant;

/**
 * Maps API keys to and from their gRPC message. Type and environment travel as their stored ordinals.
 *
 * @version ee
 */
public final class ApiKeyProtoMapper {

    private ApiKeyProtoMapper() {
    }

    public static ApiKey toApiKey(RemoteApiKey remoteApiKey) {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(Environment.values()[remoteApiKey.getEnvironment()]);

        if (remoteApiKey.hasCreatedBy()) {
            apiKey.setCreatedBy(remoteApiKey.getCreatedBy());
        }

        if (remoteApiKey.hasId()) {
            apiKey.setId(remoteApiKey.getId());
        }

        if (remoteApiKey.hasLastUsedDate()) {
            Timestamp lastUsedDate = remoteApiKey.getLastUsedDate();

            apiKey.setLastUsedDate(Instant.ofEpochSecond(lastUsedDate.getSeconds(), lastUsedDate.getNanos()));
        }

        if (remoteApiKey.hasName()) {
            apiKey.setName(remoteApiKey.getName());
        }

        if (remoteApiKey.hasSecretKey()) {
            apiKey.setSecretKey(remoteApiKey.getSecretKey());
        }

        if (remoteApiKey.hasType()) {
            apiKey.setType(PlatformType.values()[remoteApiKey.getType()]);
        }

        if (remoteApiKey.hasUserId()) {
            apiKey.setUserId(remoteApiKey.getUserId());
        }

        return apiKey;
    }

    public static RemoteApiKey toRemoteApiKey(ApiKey apiKey) {
        Environment environment = apiKey.getEnvironment();

        RemoteApiKey.Builder builder = RemoteApiKey.newBuilder()
            .setEnvironment(environment.ordinal());

        Long userId = apiKey.getUserId();

        if (userId != null) {
            builder.setUserId(userId);
        }

        if (apiKey.getCreatedBy() != null) {
            builder.setCreatedBy(apiKey.getCreatedBy());
        }

        if (apiKey.getId() != null) {
            builder.setId(apiKey.getId());
        }

        Instant lastUsedDate = apiKey.getLastUsedDate();

        if (lastUsedDate != null) {
            builder.setLastUsedDate(
                Timestamp.newBuilder()
                    .setSeconds(lastUsedDate.getEpochSecond())
                    .setNanos(lastUsedDate.getNano())
                    .build());
        }

        if (apiKey.getName() != null) {
            builder.setName(apiKey.getName());
        }

        if (apiKey.getSecretKey() != null) {
            builder.setSecretKey(apiKey.getSecretKey());
        }

        PlatformType type = apiKey.getType();

        if (type != null) {
            builder.setType(type.ordinal());
        }

        return builder.build();
    }
}
