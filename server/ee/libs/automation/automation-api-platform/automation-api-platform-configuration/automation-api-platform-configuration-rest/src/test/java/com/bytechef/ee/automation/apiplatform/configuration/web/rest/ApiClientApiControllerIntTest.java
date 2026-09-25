/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.configuration.web.rest.mapper.EnvironmentMapper;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Sends real HTTP requests to every {@link ApiClientApiController} endpoint with Spring Security's method interceptor
 * enforcing the controller's real {@code @PreAuthorize} guards through the production
 * {@link AutomationMethodSecurityConfiguration}. API clients belong to the tenant, not to a workspace, so each endpoint
 * must decide on {@link PermissionService#isTenantAdmin()} alone; an allowed request proves it entered the endpoint by
 * reaching the {@link ApiClientService} stub that throws.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ApiClientApiControllerIntTest.Config.class)
@WebMvcTest(
    controllers = ApiClientApiController.class,
    properties = {
        "bytechef.coordinator.enabled=true", "bytechef.edition=ee"
    })
class ApiClientApiControllerIntTest {

    private static final String API_CLIENT_JSON = """
        {"name": "client", "secretKey": "secret"}
        """;
    private static final String BODY_REACHED = "body reached";

    @MockitoBean
    private ApiClientService apiClientService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionService permissionService;

    static Stream<Arguments> apiClientEndpoints() {
        return Stream.of(
            Arguments.of(
                "createApiClient",
                post("/api-platform/internal/api-clients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(API_CLIENT_JSON)),
            Arguments.of("deleteApiClient", delete("/api-platform/internal/api-client/1")),
            Arguments.of("getApiClient", get("/api-platform/internal/api-client/1")),
            Arguments.of("getApiClients", get("/api-platform/internal/api-clients")),
            Arguments.of(
                "updateApiClient",
                put("/api-platform/internal/api-client/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(API_CLIENT_JSON)))
            .flatMap(arguments -> Stream.of(
                Arguments.of(arguments.get()[0], arguments.get()[1], false),
                Arguments.of(arguments.get()[0], arguments.get()[1], true)));
    }

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        when(apiClientService.create(any(ApiClient.class))).thenThrow(bodyReachedException);
        when(apiClientService.getApiClient(anyLong())).thenThrow(bodyReachedException);
        when(apiClientService.getApiClients()).thenThrow(bodyReachedException);
        when(apiClientService.update(any(ApiClient.class))).thenThrow(bodyReachedException);

        doThrow(bodyReachedException).when(apiClientService)
            .delete(anyLong());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} tenantAdmin={2}")
    @MethodSource("apiClientEndpoints")
    void testApiClientEndpointRequiresATenantAdmin(String endpointName, RequestBuilder request, boolean tenantAdmin) {
        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        if (tenantAdmin) {
            assertThatThrownBy(() -> mockMvc.perform(request))
                .as("%s must allow a tenant admin", endpointName)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> mockMvc.perform(request))
                .as("%s must deny a caller who is not a tenant admin", endpointName)
                .rootCause()
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(apiClientService);
        }

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @Configuration
    @ComponentScan(basePackages = {
        "com.bytechef.ee.automation.apiplatform.configuration.web.rest.adapter",
        "com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper"
    })
    @EnableMethodSecurity
    @ImportAutoConfiguration({
        AopAutoConfiguration.class, AutomationMethodSecurityConfiguration.class
    })
    @Import({
        ApiClientApiController.class, DateTimeMapper.class, EnvironmentMapper.class
    })
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
