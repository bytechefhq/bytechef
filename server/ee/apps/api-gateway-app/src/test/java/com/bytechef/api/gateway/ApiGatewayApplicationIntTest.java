/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.discovery.redis.client.RedisDiscoveryClient;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByLoginRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserResponse;
import com.bytechef.ee.platform.user.remote.grpc.proto.UserRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolver;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenClientInterceptor;
import com.bytechef.platform.user.scheduler.UserCleanupScheduler;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.grpc.BindableService;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
public class ApiGatewayApplicationIntTest {

    @Autowired
    private ObjectProvider<AuditEventRepository> auditEventRepositoryProvider;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private Environment environment;

    @Autowired
    private InfoEndpoint infoEndpoint;

    @Autowired
    private UserCleanupScheduler userCleanupScheduler;

    @Autowired
    private UserService userService;

    @Test
    void testAuditEventsAreSentToExecutionApp() {
        AuditEventRepository auditEventRepository = auditEventRepositoryProvider.getIfAvailable();

        assertThat(auditEventRepository).isNotNull();

        Class<?> auditEventRepositoryClass = AopUtils.getTargetClass(auditEventRepository);

        assertThat(auditEventRepositoryClass.getName()).isEqualTo(
            "com.bytechef.ee.platform.audit.remote.client.RemoteAuditEventRepositoryClient");
    }

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].id"))
            .isEqualTo("user-graphql");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testApplicationInfoHasEveryKeyTheClientReads() {
        assertThat(environment.getProperty("info.ai.copilot.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("info.ai.knowledgeBase.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("info.billing.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("info.signUp.activationRequired")).isEqualTo("false");
        assertThat(environment.getProperty("info.signUp.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("info.socialLogin.enabled")).isEqualTo("false");
    }

    @Test
    void testAuthorizationServerIsOffByDefault() {
        assertThat(environment.getProperty("bytechef.oauth2.authorization-server.enabled")).isEqualTo("false");
        assertThat(infoEndpoint.info())
            .extractingByKey("oauth2")
            .isEqualTo(Map.of("authorizationServer", Map.of("enabled", "false")));
    }

    @Test
    void testUserMailLinksPointAtThePublicUrl() {
        assertThat(environment.getProperty("bytechef.mail.base-url"))
            .isEqualTo(environment.getProperty("bytechef.public-url"));
    }

    @Test
    void testUserMailMessagesResolve() {
        assertThat(applicationContext.getMessage("email.activation.title", null, Locale.ENGLISH)).isNotBlank();
        assertThat(applicationContext.getMessage("email.invitation.title", null, Locale.ENGLISH)).isNotBlank();
        assertThat(applicationContext.getMessage("email.reset.title", null, Locale.ENGLISH)).isNotBlank();
    }

    @Test
    void testGrpcServerAcceptsConfiguredInboundMessageSize() {
        assertThat(environment.getProperty("spring.grpc.server.inbound.message.max-size")).isEqualTo("64MB");
    }

    @Test
    void testRedisDiscoveryClientIsRegistered() {
        assertThat(applicationContext.getBeansOfType(RedisDiscoveryClient.class)).hasSize(1);
    }

    @Test
    void testUserGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.platform.user.remote.v1.AuthorityRemoteService",
            "bytechef.platform.user.remote.v1.UserRemoteService");
    }

    @Test
    void testUserCleanupJobTakesItsSchedulerLock() {
        userCleanupScheduler.removeOldPersistentTokens();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.shedlock WHERE name = 'removeOldPersistentTokens'", Integer.class))
                    .isEqualTo(1);
    }

    @Test
    void testGrpcPortIsRegisteredInDiscovery() {
        assertThat(grpcPort()).isPositive();
    }

    @Test
    void testUserLookupOverTheGrpcPort() {
        ApiGatewayLoginIntTest.createActivatedUser(userService, "grpc-user@localhost.com", "secret-password");

        ManagedChannel managedChannel = channel(new ServiceTokenClientInterceptor("test-service-token"));

        try {
            FetchUserResponse fetchUserResponse = UserRemoteServiceGrpc.newBlockingStub(managedChannel)
                .fetchUserByLogin(
                    FetchUserByLoginRequest.newBuilder()
                        .setLogin("grpc-user@localhost.com")
                        .build());

            assertThat(fetchUserResponse.getUser()
                .getLogin()).isEqualTo("grpc-user@localhost.com");
        } finally {
            managedChannel.shutdownNow();
        }
    }

    @Test
    void testUserLookupWithoutServiceTokenIsUnauthenticated() {
        ManagedChannel managedChannel = channel(new ServiceTokenClientInterceptor("wrong-token"));

        try {
            assertThatThrownBy(() -> UserRemoteServiceGrpc.newBlockingStub(managedChannel)
                .fetchUserByLogin(
                    FetchUserByLoginRequest.newBuilder()
                        .setLogin("grpc-user@localhost.com")
                        .build()))
                            .isInstanceOfSatisfying(StatusRuntimeException.class,
                                exception -> assertThat(exception.getStatus()
                                    .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        } finally {
            managedChannel.shutdownNow();
        }
    }

    private ManagedChannel channel(ClientInterceptor clientInterceptor) {
        return ManagedChannelBuilder.forAddress("localhost", grpcPort())
            .usePlaintext()
            .intercept(clientInterceptor)
            .build();
    }

    private int grpcPort() {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("api-gateway-app");

        return serviceInstances.stream()
            .map(ServiceInstance::getMetadata)
            .map(metadata -> metadata.get(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY))
            .filter(grpcPort -> grpcPort != null)
            .map(Integer::parseInt)
            .findFirst()
            .orElse(0);
    }
}
