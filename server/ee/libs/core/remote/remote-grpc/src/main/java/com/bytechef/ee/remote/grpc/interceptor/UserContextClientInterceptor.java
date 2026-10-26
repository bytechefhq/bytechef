/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Sends the login and authorities of the caller's authenticated user with every outgoing call.
 *
 * @version ee
 */
public class UserContextClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        String login = getLogin(authentication);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {
                if (login != null) {
                    metadata.put(GrpcMetadataKeys.USER_LOGIN, login);
                    metadata.put(
                        GrpcMetadataKeys.USER_AUTHORITIES,
                        authentication.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(",")));
                }

                super.start(responseListener, metadata);
            }
        };
    }

    private static String getLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {

            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String login) {
            return login;
        }

        return null;
    }
}
