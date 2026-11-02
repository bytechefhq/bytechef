/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.ee.remote.grpc.EchoService;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.ee.remote.grpc.interceptor.GrpcMetadataKeys;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextServerInterceptor;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
class ConnectedUserRemotePrincipalCodecTest {

    private final ConnectedUserRemotePrincipalCodec connectedUserRemotePrincipalCodec =
        new ConnectedUserRemotePrincipalCodec();

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsRebuiltAsConnectedUser() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new LocalConnectedUserAuthentication("external-user-1", 7L, 2L, true));

        assertThat(describe(echoWithCodec())).isEqualTo("external-user-1|7|2|false|EMBEDDED_API_KEY|EMBEDDED");
    }

    @Test
    void testConnectedUserSurvivesASecondHop() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new RemoteConnectedUserAuthenticationToken(
                    2L, 7L, new User("external-user-1", "", List.of()), true,
                    CredentialType.EMBEDDED_MCP_SERVER_API_KEY, null));

        assertThat(describe(echoWithCodec())).isEqualTo("external-user-1|7|2|true|EMBEDDED_MCP_SERVER_API_KEY|null");
    }

    @Test
    void testUnauthenticatedConnectedUserCarriesNoIdentity() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new LocalConnectedUserAuthentication("external-user-1", 7L, 2L, false));

        assertThat(echoWithCodec()).isNull();
    }

    @Test
    void testMalformedConnectedUserIdBindsNoIdentity() throws Exception {
        assertThat(echoWithMetadata(metadata -> {
            putConnectedUser(metadata, "seven", "2", "false");
        })).isNull();
    }

    @Test
    void testMalformedEnvironmentIdBindsNoIdentity() throws Exception {
        assertThat(echoWithMetadata(metadata -> {
            putConnectedUser(metadata, "7", "production", "false");
        })).isNull();
    }

    @Test
    void testNonBooleanApiKeyAuthenticatedBindsNoIdentity() throws Exception {
        assertThat(echoWithMetadata(metadata -> {
            putConnectedUser(metadata, "7", "2", "yes");
        })).isNull();
    }

    @Test
    void testMissingCredentialTypeBindsNoIdentity() throws Exception {
        assertThat(echoWithMetadata(metadata -> {
            putConnectedUser(metadata, "7", "2", "false");

            metadata.removeAll(GrpcMetadataKeys.CREDENTIAL_TYPE);
        })).isNull();
    }

    @Test
    void testWellFormedMetadataBindsTheConnectedUser() throws Exception {
        assertThat(describe(echoWithMetadata(metadata -> {
            putConnectedUser(metadata, "7", "2", "false");
        }))).isEqualTo("external-user-1|7|2|false|EMBEDDED_API_KEY|null");
    }

    private Authentication echoWithCodec() throws Exception {
        EchoService echoService = new EchoService();

        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            echoService, List.of(new UserContextServerInterceptor(List.of(connectedUserRemotePrincipalCodec))),
            List.of(new UserContextClientInterceptor(List.of(connectedUserRemotePrincipalCodec))))) {

            EchoService.echoPrincipal(inProcessGrpc.channel());
        }

        return echoService.getLastAuthentication();
    }

    private Authentication echoWithMetadata(Consumer<Metadata> metadataWriter) throws Exception {
        EchoService echoService = new EchoService();

        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            echoService, List.of(new UserContextServerInterceptor(List.of(connectedUserRemotePrincipalCodec))),
            List.of(metadataInterceptor(metadataWriter)))) {

            EchoService.echoPrincipal(inProcessGrpc.channel());
        }

        return echoService.getLastAuthentication();
    }

    private static String describe(Authentication authentication) {
        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken =
            (AbstractApiKeyAuthenticationToken) authentication;

        return connectedUserAuthentication.externalUserId() + "|" + connectedUserAuthentication.connectedUserId() +
            "|" + connectedUserAuthentication.environmentId() + "|" +
            connectedUserAuthentication.apiKeyAuthenticated() + "|" + apiKeyAuthenticationToken.getCredentialType() +
            "|" + apiKeyAuthenticationToken.getPlatformType();
    }

    private static ClientInterceptor metadataInterceptor(Consumer<Metadata> metadataWriter) {
        return new ClientInterceptor() {

            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                    channel.newCall(methodDescriptor, callOptions)) {

                    @Override
                    public void start(Listener<RespT> responseListener, Metadata metadata) {
                        metadataWriter.accept(metadata);

                        super.start(responseListener, metadata);
                    }
                };
            }
        };
    }

    private static void putConnectedUser(
        Metadata metadata, String connectedUserId, String environmentId, String apiKeyAuthenticated) {

        metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "connected-user");
        metadata.put(GrpcMetadataKeys.USER_LOGIN, "external-user-1".getBytes(StandardCharsets.UTF_8));
        metadata.put(ConnectedUserRemotePrincipalCodec.CONNECTED_USER_ID, connectedUserId);
        metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, environmentId);
        metadata.put(ConnectedUserRemotePrincipalCodec.API_KEY_AUTHENTICATED, apiKeyAuthenticated);
        metadata.put(GrpcMetadataKeys.CREDENTIAL_TYPE, CredentialType.EMBEDDED_API_KEY.name());
    }

    private static final class LocalConnectedUserAuthentication extends AbstractApiKeyAuthenticationToken
        implements ConnectedUserAuthentication {

        private final long connectedUserId;
        private final long environmentId;
        private final String externalUserId;

        LocalConnectedUserAuthentication(
            String externalUserId, long connectedUserId, long environmentId, boolean authenticated) {

            this.connectedUserId = connectedUserId;
            this.environmentId = environmentId;
            this.externalUserId = externalUserId;

            setAuthenticated(authenticated);
        }

        @Override
        public long connectedUserId() {
            return connectedUserId;
        }

        @Override
        public long environmentId() {
            return environmentId;
        }

        @Override
        public String externalUserId() {
            return externalUserId;
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.EMBEDDED_API_KEY;
        }

        @Override
        public PlatformType getPlatformType() {
            return PlatformType.EMBEDDED;
        }

        @Override
        public Object getPrincipal() {
            return externalUserId;
        }
    }
}
