/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.security;

import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.ee.remote.grpc.interceptor.GrpcMetadataKeys;
import com.bytechef.ee.remote.grpc.security.RemoteCredentialMetadata;
import com.bytechef.ee.remote.grpc.security.RemotePrincipalCodec;
import com.bytechef.platform.security.web.authentication.CredentialType;
import io.grpc.Metadata;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

/**
 * Carries an embedded connected user across gRPC calls, so connected-user checks still run in the receiving app.
 *
 * @version ee
 */
@Component
public class ConnectedUserRemotePrincipalCodec implements RemotePrincipalCodec {

    static final Metadata.Key<String> API_KEY_AUTHENTICATED =
        Metadata.Key.of("x-bytechef-api-key-authenticated", Metadata.ASCII_STRING_MARSHALLER);

    static final Metadata.Key<String> CONNECTED_USER_ID =
        Metadata.Key.of("x-bytechef-connected-user-id", Metadata.ASCII_STRING_MARSHALLER);

    static final String PRINCIPAL_KIND = "connected-user";

    @Override
    public String getPrincipalKind() {
        return PRINCIPAL_KIND;
    }

    @Override
    public boolean supports(Authentication authentication) {
        return authentication instanceof ConnectedUserAuthentication;
    }

    @Override
    public boolean encode(Authentication authentication, Metadata metadata) {
        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        String externalUserId = connectedUserAuthentication.externalUserId();

        if (externalUserId == null) {
            return false;
        }

        metadata.put(GrpcMetadataKeys.USER_LOGIN, externalUserId.getBytes(StandardCharsets.UTF_8));
        metadata.put(CONNECTED_USER_ID, String.valueOf(connectedUserAuthentication.connectedUserId()));
        metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, String.valueOf(connectedUserAuthentication.environmentId()));
        metadata.put(API_KEY_AUTHENTICATED, String.valueOf(connectedUserAuthentication.apiKeyAuthenticated()));

        return true;
    }

    @Override
    public @Nullable Authentication decode(String login, Metadata metadata) {
        String apiKeyAuthenticated = metadata.get(API_KEY_AUTHENTICATED);
        Long connectedUserId = parseLong(metadata.get(CONNECTED_USER_ID));
        CredentialType credentialType = RemoteCredentialMetadata.getCredentialType(metadata);
        Long environmentId = parseLong(metadata.get(GrpcMetadataKeys.ENVIRONMENT_ID));

        if (connectedUserId == null || credentialType == null || environmentId == null ||
            !("true".equals(apiKeyAuthenticated) || "false".equals(apiKeyAuthenticated))) {

            return null;
        }

        return new RemoteConnectedUserAuthenticationToken(
            environmentId, connectedUserId, new User(login, "", List.of()), Boolean.parseBoolean(apiKeyAuthenticated),
            credentialType, RemoteCredentialMetadata.getPlatformType(metadata));
    }

    private static @Nullable Long parseLong(@Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }
}
