/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import com.bytechef.ee.remote.grpc.security.RemoteCredentialMetadata;
import com.bytechef.ee.remote.grpc.security.RemotePrincipalCodec;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.ExternalIssuerAuthentication;
import com.bytechef.platform.security.web.authentication.PlatformUserAuthentication;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Sends the caller's principal with every outgoing call: an interactively authenticated platform user, a
 * {@link PlatformUserAuthentication}, an {@link ExternalIssuerAuthentication} or an API-key user with their login and
 * authorities, or a principal kind written by a {@link RemotePrincipalCodec}.
 *
 * @version ee
 */
public class UserContextClientInterceptor implements ClientInterceptor {

    private final List<RemotePrincipalCodec> remotePrincipalCodecs;

    public UserContextClientInterceptor() {
        this(List.of());
    }

    public UserContextClientInterceptor(List<RemotePrincipalCodec> remotePrincipalCodecs) {
        this.remotePrincipalCodecs = List.copyOf(remotePrincipalCodecs);
    }

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

    private void putPrincipal(Authentication authentication, Metadata metadata) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        for (RemotePrincipalCodec remotePrincipalCodec : remotePrincipalCodecs) {
            if (remotePrincipalCodec.supports(authentication)) {
                if (remotePrincipalCodec.encode(authentication, metadata)) {
                    metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, remotePrincipalCodec.getPrincipalKind());

                    if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
                        RemoteCredentialMetadata.put(apiKeyAuthenticationToken, metadata);
                    }
                }

                return;
            }
        }

        if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            putApiKeyUser(apiKeyAuthenticationToken, metadata);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken ||
            authentication instanceof RememberMeAuthenticationToken ||
            authentication instanceof PlatformUserAuthentication) {

            putUser(authentication, PrincipalKind.USER, metadata);
        } else if (authentication instanceof ExternalIssuerAuthentication) {
            putUser(authentication, PrincipalKind.EXTERNAL_ISSUER, metadata);
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

        RemoteCredentialMetadata.put(apiKeyAuthenticationToken, metadata);

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

    private static void putUser(Authentication authentication, String principalKind, Metadata metadata) {
        String login = getLogin(authentication.getPrincipal());

        if (login == null) {
            return;
        }

        metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, principalKind);
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
