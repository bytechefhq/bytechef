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

    public static final Metadata.Key<String> CREDENTIAL_TYPE =
        Metadata.Key.of("x-bytechef-credential-type", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> ENVIRONMENT_ID =
        Metadata.Key.of("x-bytechef-environment-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> PLATFORM_TYPE =
        Metadata.Key.of("x-bytechef-platform-type", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> PRINCIPAL_KIND =
        Metadata.Key.of("x-bytechef-principal-kind", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> SERVICE_TOKEN =
        Metadata.Key.of("x-bytechef-service-token", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> TENANT_ID =
        Metadata.Key.of("x-bytechef-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<byte[]> USER_AUTHORITY =
        Metadata.Key.of("x-bytechef-user-authority-bin", Metadata.BINARY_BYTE_MARSHALLER);

    public static final Metadata.Key<byte[]> USER_LOGIN =
        Metadata.Key.of("x-bytechef-user-login-bin", Metadata.BINARY_BYTE_MARSHALLER);

    private GrpcMetadataKeys() {
    }
}
