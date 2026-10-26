/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs each incoming call as the user its caller sent, trusted because the service token already admitted the call.
 *
 * @version ee
 */
public class UserContextServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        byte[] login = metadata.get(GrpcMetadataKeys.USER_LOGIN);
        Iterable<byte[]> authorities = metadata.getAll(GrpcMetadataKeys.USER_AUTHORITY);

        return new UserBindingListener<>(
            serverCallHandler.startCall(serverCall, metadata),
            login == null ? null : new String(login, StandardCharsets.UTF_8), toGrantedAuthorities(authorities));
    }

    private static final class UserBindingListener<ReqT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final List<GrantedAuthority> authorities;
        private final String login;

        private UserBindingListener(
            ServerCall.Listener<ReqT> delegate, String login, List<GrantedAuthority> authorities) {

            super(delegate);

            this.authorities = authorities;
            this.login = login;
        }

        @Override
        public void onHalfClose() {
            if (StringUtils.isBlank(login)) {
                super.onHalfClose();

                return;
            }

            SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(login, null, authorities));

            SecurityContextHolder.setContext(securityContext);

            try {
                super.onHalfClose();
            } finally {
                SecurityContextHolder.setContext(originalSecurityContext);
            }
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
