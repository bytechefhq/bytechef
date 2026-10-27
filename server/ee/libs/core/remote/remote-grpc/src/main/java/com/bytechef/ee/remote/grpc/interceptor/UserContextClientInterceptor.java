/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
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

        return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {
                putPrincipal(authentication, metadata);

                super.start(responseListener, metadata);
            }
        };
    }

    private static void putPrincipal(Authentication authentication, Metadata metadata) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        if (authentication instanceof ConnectedUserAuthentication connectedUserAuthentication) {
            putConnectedUser(connectedUserAuthentication, metadata);
        } else if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            putApiKeyUser(apiKeyAuthenticationToken, metadata);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken ||
            authentication instanceof RememberMeAuthenticationToken) {

            putUser(authentication, metadata);
        }
    }

    private static void putApiKeyUser(
        AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken, Metadata metadata) {

        String login = getLogin(apiKeyAuthenticationToken.getPrincipal());

        if (login == null) {
            return;
        }

        metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, PrincipalKind.API_KEY);
        metadata.put(GrpcMetadataKeys.USER_LOGIN, login.getBytes(StandardCharsets.UTF_8));
        metadata.put(
            GrpcMetadataKeys.ENVIRONMENT_ID, String.valueOf(apiKeyAuthenticationToken.getEnvironmentId()));

        putAuthorities(apiKeyAuthenticationToken, metadata);
    }

    private static void putAuthorities(Authentication authentication, Metadata metadata) {
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            String authority = grantedAuthority.getAuthority();

            if (authority != null) {
                metadata.put(GrpcMetadataKeys.USER_AUTHORITY, authority.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void putConnectedUser(
        ConnectedUserAuthentication connectedUserAuthentication, Metadata metadata) {

        String externalUserId = connectedUserAuthentication.externalUserId();

        if (externalUserId == null) {
            return;
        }

        metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, PrincipalKind.CONNECTED_USER);
        metadata.put(GrpcMetadataKeys.USER_LOGIN, externalUserId.getBytes(StandardCharsets.UTF_8));
        metadata.put(
            GrpcMetadataKeys.CONNECTED_USER_ID, String.valueOf(connectedUserAuthentication.connectedUserId()));
        metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, String.valueOf(connectedUserAuthentication.environmentId()));
        metadata.put(
            GrpcMetadataKeys.API_KEY_AUTHENTICATED,
            String.valueOf(connectedUserAuthentication.apiKeyAuthenticated()));
    }

    private static void putUser(Authentication authentication, Metadata metadata) {
        String login = getLogin(authentication.getPrincipal());

        if (login == null) {
            return;
        }

        metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, PrincipalKind.USER);
        metadata.put(GrpcMetadataKeys.USER_LOGIN, login.getBytes(StandardCharsets.UTF_8));

        putAuthorities(authentication, metadata);
    }

    private static String getLogin(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String login) {
            return login;
        }

        return null;
    }
}
