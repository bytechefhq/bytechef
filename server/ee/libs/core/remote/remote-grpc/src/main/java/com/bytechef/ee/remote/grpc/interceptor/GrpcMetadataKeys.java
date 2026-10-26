/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import io.grpc.Metadata;

/**
 * Metadata keys every call between microservices carries.
 *
 * @version ee
 */
public final class GrpcMetadataKeys {

    public static final Metadata.Key<String> SERVICE_TOKEN =
        Metadata.Key.of("x-bytechef-service-token", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> TENANT_ID =
        Metadata.Key.of("x-bytechef-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> USER_AUTHORITIES =
        Metadata.Key.of("x-bytechef-user-authorities", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> USER_LOGIN =
        Metadata.Key.of("x-bytechef-user-login", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMetadataKeys() {
    }
}
