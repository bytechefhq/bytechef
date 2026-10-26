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
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Sends the login and authorities of the caller's interactively authenticated platform user with every outgoing call;
 * API-key and connected-user principals are not carried.
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
                    metadata.put(GrpcMetadataKeys.USER_LOGIN, login.getBytes(StandardCharsets.UTF_8));

                    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
                        String authority = grantedAuthority.getAuthority();

                        if (authority != null) {
                            metadata.put(
                                GrpcMetadataKeys.USER_AUTHORITY, authority.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }

                super.start(responseListener, metadata);
            }
        };
    }

    private static String getLogin(Authentication authentication) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken ||
            authentication instanceof RememberMeAuthenticationToken) || !authentication.isAuthenticated()) {

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
