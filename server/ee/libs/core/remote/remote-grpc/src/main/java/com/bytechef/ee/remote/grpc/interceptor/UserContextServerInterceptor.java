/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import com.bytechef.ee.remote.grpc.security.RemoteApiKeyAuthenticationToken;
import com.bytechef.ee.remote.grpc.security.RemoteConnectedUserAuthenticationToken;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Runs each incoming call as the user its caller sent, trusted because the service token already admitted the call.
 *
 * @version ee
 */
public class UserContextServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        return new UserBindingListener<>(serverCallHandler.startCall(serverCall, metadata), toAuthentication(metadata));
    }

    private static final class UserBindingListener<ReqT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Authentication authentication;

        private UserBindingListener(ServerCall.Listener<ReqT> delegate, Authentication authentication) {
            super(delegate);

            this.authentication = authentication;
        }

        @Override
        public void onHalfClose() {
            if (authentication == null) {
                super.onHalfClose();

                return;
            }

            SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(authentication);

            SecurityContextHolder.setContext(securityContext);

            try {
                super.onHalfClose();
            } finally {
                SecurityContextHolder.setContext(originalSecurityContext);
            }
        }
    }

    private static Authentication toAuthentication(Metadata metadata) {
        byte[] loginBytes = metadata.get(GrpcMetadataKeys.USER_LOGIN);
        String principalKind = metadata.get(GrpcMetadataKeys.PRINCIPAL_KIND);

        if (loginBytes == null || principalKind == null) {
            return null;
        }

        String login = new String(loginBytes, StandardCharsets.UTF_8);

        if (StringUtils.isBlank(login)) {
            return null;
        }

        List<GrantedAuthority> authorities = toGrantedAuthorities(metadata.getAll(GrpcMetadataKeys.USER_AUTHORITY));

        return switch (principalKind) {
            case PrincipalKind.USER -> UsernamePasswordAuthenticationToken.authenticated(login, null, authorities);
            case PrincipalKind.API_KEY -> toApiKeyAuthentication(login, authorities, metadata);
            case PrincipalKind.CONNECTED_USER -> toConnectedUserAuthentication(login, metadata);
            default -> null;
        };
    }

    private static Authentication toApiKeyAuthentication(
        String login, List<GrantedAuthority> authorities, Metadata metadata) {

        Long environmentId = parseLong(metadata.get(GrpcMetadataKeys.ENVIRONMENT_ID));

        if (environmentId == null) {
            return null;
        }

        return new RemoteApiKeyAuthenticationToken(environmentId, new User(login, "", authorities));
    }

    private static Authentication toConnectedUserAuthentication(String externalUserId, Metadata metadata) {
        String apiKeyAuthenticated = metadata.get(GrpcMetadataKeys.API_KEY_AUTHENTICATED);
        Long connectedUserId = parseLong(metadata.get(GrpcMetadataKeys.CONNECTED_USER_ID));
        Long environmentId = parseLong(metadata.get(GrpcMetadataKeys.ENVIRONMENT_ID));

        if (connectedUserId == null || environmentId == null ||
            !("true".equals(apiKeyAuthenticated) || "false".equals(apiKeyAuthenticated))) {

            return null;
        }

        return new RemoteConnectedUserAuthenticationToken(
            environmentId, connectedUserId, new User(externalUserId, "", List.of()),
            Boolean.parseBoolean(apiKeyAuthenticated));
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    private static List<GrantedAuthority> toGrantedAuthorities(Iterable<byte[]> authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        if (authorities == null) {
            return grantedAuthorities;
        }

        for (byte[] authority : authorities) {
            grantedAuthorities.add(new SimpleGrantedAuthority(new String(authority, StandardCharsets.UTF_8)));
        }

        return grantedAuthorities;
    }
}
