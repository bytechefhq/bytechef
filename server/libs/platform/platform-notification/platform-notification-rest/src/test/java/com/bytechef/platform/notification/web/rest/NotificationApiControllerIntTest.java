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

package com.bytechef.platform.notification.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.facade.NotificationFacade;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.notification.web.rest.model.NotificationModel;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    NotificationApiController.class, NotificationApiControllerIntTest.MethodSecurityConfiguration.class
})
@MockitoBean(types = NotificationFacade.class)
@WebMvcTest(controllers = NotificationApiController.class, properties = "bytechef.coordinator.enabled=true")
class NotificationApiControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long NOTIFICATION_ID = 3L;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private NotificationApi notificationApi;

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        doThrow(bodyReachedException).when(conversionService)
            .convert(any(), eq(Notification.class));
        doThrow(bodyReachedException).when(notificationService)
            .delete(anyLong());
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("notificationWrites")
    void testNotificationWriteDeniesACallerWithoutTheAdminAuthority(GuardedOperation guardedOperation) {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> guardedOperation.invoke(notificationApi))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(permissionService);
    }

    @ParameterizedTest
    @MethodSource("notificationWrites")
    void testNotificationWriteAllowsACallerHoldingTheAdminAuthority(GuardedOperation guardedOperation) {
        authenticate(AuthorityConstants.ADMIN);

        assertThatThrownBy(() -> guardedOperation.invoke(notificationApi))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verifyNoInteractions(permissionService);
    }

    static Stream<Named<GuardedOperation>> notificationWrites() {
        return Stream.of(
            Named.of(
                "createNotification",
                (GuardedOperation) guardedApi -> guardedApi.createNotification(new NotificationModel())),
            Named.of(
                "deleteNotification",
                (GuardedOperation) guardedApi -> guardedApi.deleteNotification(NOTIFICATION_ID)),
            Named.of(
                "updateNotification",
                (GuardedOperation) guardedApi -> guardedApi.updateNotification(
                    NOTIFICATION_ID, new NotificationModel())));
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "credentials", List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }

    @FunctionalInterface
    interface GuardedOperation {

        void invoke(NotificationApi guardedApi);
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean
        @Primary
        ConversionService conversionService() {
            return mock(ConversionService.class, MockReset.withSettings(MockReset.AFTER));
        }

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
        }
    }
}
