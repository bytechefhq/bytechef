/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.ee.platform.user.remote.client.service.RemoteUserServiceClient;
import com.bytechef.ee.platform.user.remote.grpc.UserRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenServerInterceptor;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConfigurationApplication.class, properties = {
        "bytechef.mail.host=mailpit", "bytechef.mail.port=1025"
    })
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
public class ConfigurationApplicationIntTest {

    private static final UserService GATEWAY_USER_SERVICE = mock(UserService.class);

    private static Server gatewayServer;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private UserService userService;

    @AfterAll
    static void afterAll() {
        gatewayServer.shutdownNow();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        try {
            gatewayServer = NettyServerBuilder.forPort(0)
                .addService(
                    ServerInterceptors.intercept(
                        new UserRemoteServiceGrpcServer(GATEWAY_USER_SERVICE),
                        new ServiceTokenServerInterceptor("test-service-token")))
                .build()
                .start();
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        registry.add(
            "bytechef.remote.grpc.targets.api-gateway-app", () -> "localhost:" + gatewayServer.getPort());
    }

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix"))
            .isEqualTo("configuration-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testMailUsesBytechefMailSettings() {
        assertThat(environment.getProperty("spring.mail.host")).isEqualTo("mailpit");
        assertThat(environment.getProperty("spring.mail.port")).isEqualTo("1025");
    }

    @Test
    void testJdbcConvertsEncryptedMaps() {
        JdbcCustomConversions jdbcCustomConversions = applicationContext.getBean(JdbcCustomConversions.class);

        assertThat(jdbcCustomConversions.hasCustomWriteTarget(EncryptedMapWrapper.class)).isTrue();
        assertThat(jdbcCustomConversions.hasCustomReadTarget(String.class, EncryptedMapWrapper.class)).isTrue();
    }

    @Test
    void testApiKeyAndConnectedUserGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.embedded.connected.user.remote.v1.ConnectedUserRemoteService",
            "bytechef.platform.security.remote.v1.ApiKeyRemoteService");
        assertThat(serviceNames).doesNotContain(
            "bytechef.platform.user.remote.v1.UserRemoteService",
            "bytechef.platform.component.remote.v1.ComponentDefinitionRemoteService",
            "bytechef.platform.workflow.task.dispatcher.remote.v1.TaskDispatcherDefinitionRemoteService");
    }

    @Test
    void testUserServiceCallsTheGatewayOverGrpc() {
        User user = new User();

        user.setId(1L);
        user.setLogin("admin@localhost.com");

        when(GATEWAY_USER_SERVICE.fetchUserByLogin("admin@localhost.com")).thenReturn(Optional.of(user));

        assertThat(userService).isInstanceOf(RemoteUserServiceClient.class);
        assertThat(userService.getUser("admin@localhost.com")
            .getId()).isEqualTo(1L);
    }

    @Nested
    @TestPropertySource(properties = "bytechef.message-broker.provider=amqp")
    class AmqpMessageBrokerProvider {

        @Autowired
        private MessageBroker messageBroker;

        @Test
        void testAmqpMessageBrokerIsAvailable() {
            Class<?> messageBrokerClass = messageBroker.getClass();

            assertThat(messageBrokerClass.getName()).isEqualTo("com.bytechef.message.broker.amqp.AmqpMessageBroker");
        }
    }
}
