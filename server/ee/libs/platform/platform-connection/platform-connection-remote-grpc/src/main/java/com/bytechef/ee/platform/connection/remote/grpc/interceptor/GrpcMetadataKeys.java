/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.interceptor;

import com.bytechef.tenant.constant.TenantConstants;
import io.grpc.Metadata;

/**
 * Shared gRPC metadata keys mirroring the {@code /remote/**} REST headers (see
 * {@code RemoteServiceAuthenticationFilter} + {@code RemoteMultiTenantFilter}). gRPC lowercases header names, so both
 * client and server must derive their keys from the same {@link TenantConstants} strings.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class GrpcMetadataKeys {

    public static final Metadata.Key<String> INTERNAL_SERVICE_TOKEN =
        Metadata.Key.of(TenantConstants.INTERNAL_SERVICE_TOKEN, Metadata.ASCII_STRING_MARSHALLER);

    public static final Metadata.Key<String> CURRENT_TENANT_ID =
        Metadata.Key.of(TenantConstants.CURRENT_TENANT_ID, Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMetadataKeys() {
    }
}
