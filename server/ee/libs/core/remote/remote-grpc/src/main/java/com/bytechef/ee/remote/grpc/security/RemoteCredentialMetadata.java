/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import com.bytechef.ee.remote.grpc.interceptor.GrpcMetadataKeys;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import io.grpc.Metadata;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class RemoteCredentialMetadata {

    private RemoteCredentialMetadata() {
    }

    public static void put(AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken, Metadata metadata) {
        CredentialType credentialType = apiKeyAuthenticationToken.getCredentialType();

        metadata.put(GrpcMetadataKeys.CREDENTIAL_TYPE, credentialType.name());

        PlatformType platformType = apiKeyAuthenticationToken.getPlatformType();

        if (platformType != null) {
            metadata.put(GrpcMetadataKeys.PLATFORM_TYPE, platformType.name());
        }
    }

    public static @Nullable CredentialType getCredentialType(Metadata metadata) {
        return valueOf(CredentialType.class, metadata.get(GrpcMetadataKeys.CREDENTIAL_TYPE));
    }

    public static @Nullable PlatformType getPlatformType(Metadata metadata) {
        return valueOf(PlatformType.class, metadata.get(GrpcMetadataKeys.PLATFORM_TYPE));
    }

    private static <E extends Enum<E>> @Nullable E valueOf(Class<E> enumClass, @Nullable String name) {
        if (name == null) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}
