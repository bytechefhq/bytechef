/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.exception.AbstractErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppApiController.class)
@WithMockUser
class AppApiControllerIntTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DEPLOYED_DATE = Instant.parse("2026-01-02T00:00:00Z");

    @MockitoBean
    private AppApiCollectionFacade appApiCollectionFacade;

    @MockitoBean
    private AppFacade appFacade;

    @MockitoBean
    private AppWorkflowFacade appWorkflowFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAppBindableConnectionsPassesTheComponentFilterAndReturnsUsable() throws Exception {
        when(appFacade.getBindableConnections(1L, "slack")).thenReturn(
            List.of(
                new AppBindableConnectionDTO(101L, "Slack A", "slack", 1, 2, true),
                new AppBindableConnectionDTO(102L, "Slack B", "slack", 1, 2, false)));

        mockMvc.perform(get("/v1/apps/1/bindable-connections").param("componentName", "slack"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(101))
            .andExpect(jsonPath("$[0].name").value("Slack A"))
            .andExpect(jsonPath("$[0].componentName").value("slack"))
            .andExpect(jsonPath("$[0].connectionVersion").value(1))
            .andExpect(jsonPath("$[0].componentVersion").value(2))
            .andExpect(jsonPath("$[0].usable").value(true))
            .andExpect(jsonPath("$[1].usable").value(false));
    }

    @Test
    void testGetAppBindableConnectionsWithoutAFilterPassesNull() throws Exception {
        when(appFacade.getBindableConnections(1L, null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/apps/1/bindable-connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(appFacade).getBindableConnections(1L, null);
    }

    @Test
    void testGetAppBindableConnectionsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(appFacade.getBindableConnections(1L, null)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/apps/1/bindable-connections"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testGetAppComponentOperationsReturnsActionAndToolNames() throws Exception {
        when(appFacade.getComponentOperations("slack", 2)).thenReturn(
            new AppComponentOperationsDTO(List.of("sendMessage"), List.of("sendMessageTool")));

        mockMvc.perform(
            get("/v1/apps/component-operations").param("componentName", "slack")
                .param("componentVersion", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actionNames[0]").value("sendMessage"))
            .andExpect(jsonPath("$.toolNames[0]").value("sendMessageTool"));
    }

    @Test
    void testGetAppComponentOperationsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(appFacade.getComponentOperations("slack", 2)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(
            get("/v1/apps/component-operations").param("componentName", "slack")
                .param("componentVersion", "2"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @ValueSource(strings = {
        "development", "Development", "DEVELOPMENT", " development "
    })
    void testGetAppsAcceptsAnyCaseOfTheEnvironmentHeader(String requestEnvironment) throws Exception {
        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk());

        verify(appFacade).getApps(7L, Environment.DEVELOPMENT.ordinal());
    }

    @ParameterizedTest(name = "X-Environment [{0}]")
    @ValueSource(strings = {
        "", "   "
    })
    void testGetAppsTreatsABlankEnvironmentHeaderAsAbsent(String requestEnvironment) throws Exception {
        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk());

        verify(appFacade).getApps(7L, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testGetAppsRejectsAnUnknownEnvironmentHeader() throws Exception {
        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", "staging-x"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appFacade);
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @ValueSource(strings = {
        "development", "Development"
    })
    void testByIdRouteAcceptsAnyCaseOfTheEnvironmentHeader(String requestEnvironment) throws Exception {
        givenApp();

        mockMvc.perform(get("/v1/apps/1").header("X-Environment", requestEnvironment))
            .andExpect(status().isOk());

        verify(appFacade).requireAppInEnvironment(1L, Environment.DEVELOPMENT);
    }

    @Test
    void testByIdRouteRejectsAnUnknownEnvironmentHeader() throws Exception {
        mockMvc.perform(get("/v1/apps/1").header("X-Environment", "staging-x"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appFacade);
    }

    @Test
    void testCreateAppReturnsCreatedWithSecretKey() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.STAGING, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE))
                    .thenReturn(new AppSecretKeyDTO(appDto(AppStatus.ENABLED, Environment.STAGING), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "STAGING")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"description\":\"desc\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.secretKey").value("secret"))
            .andExpect(jsonPath("$.app.id").value(1))
            .andExpect(jsonPath("$.app.environment").value("STAGING"))
            .andExpect(jsonPath("$.app.secretKeyHint").value("hint1234"));
    }

    @Test
    void testCreateAppWithANameLongerThanTheColumnIsRejectedBeforeTheFacade() throws Exception {
        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "STAGING")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + "x".repeat(257) + "\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value(containsString("name")));

        verifyNoInteractions(appFacade);
    }

    @Test
    void testCreateAppDefaultsEnvironmentToProductionAndPassesVisibility() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.PRODUCTION, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE))
                    .thenReturn(new AppSecretKeyDTO(appDto(AppStatus.ENABLED), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"SELF_HOSTED\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void testGetAppsResolvesEnvironmentOrdinal() throws Exception {
        when(appFacade.getApps(7L, (long) Environment.STAGING.ordinal()))
            .thenReturn(List.of(appDto(AppStatus.ENABLED)));

        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", "STAGING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Sales"))
            .andExpect(jsonPath("$[0].status").value("ENABLED"))
            .andExpect(jsonPath("$[0].hostingTarget").value("VERCEL"))
            .andExpect(jsonPath("$[0].visibility").value("WORKSPACE"));
    }

    @Test
    void testGetAppReturnsApp() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(appDto(AppStatus.ENABLED));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uuid").value("uuid-1"))
            .andExpect(jsonPath("$.workspaceId").value(7));
    }

    @Test
    void testUpdateAppPassesGivenFieldsAndLeavesTheRestNull() throws Exception {
        givenApp();

        when(
            appFacade.updateApp(
                1L, "Renamed", null, null, "https://example.com", DEPLOYED_DATE, null))
                    .thenReturn(appDto(AppStatus.ENABLED));

        mockMvc
            .perform(
                patch("/v1/apps/1").contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Renamed\",\"url\":\"https://example.com\"," +
                            "\"lastDeployedDate\":\"2026-01-02T00:00:00Z\"}"))
            .andExpect(status().isOk());

        verify(appFacade, never()).updateAppStatus(anyLong(), any());
    }

    @Test
    void testUpdateAppWithStatusOnlyCallsUpdateAppStatus() throws Exception {
        givenApp();

        when(appFacade.updateAppStatus(1L, AppStatus.DISABLED)).thenReturn(appDto(AppStatus.DISABLED));

        mockMvc
            .perform(patch("/v1/apps/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(appFacade, never()).updateApp(
            anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUpdateAppWithFieldsAndStatusReturnsTheStatusUpdatedApp() throws Exception {
        givenApp();

        when(appFacade.updateApp(1L, null, "", null, null, null, ResourceVisibility.PRIVATE))
            .thenReturn(appDto(AppStatus.ENABLED));
        when(appFacade.updateAppStatus(1L, AppStatus.DISABLED)).thenReturn(appDto(AppStatus.DISABLED));

        mockMvc
            .perform(
                patch("/v1/apps/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"\",\"visibility\":\"PRIVATE\",\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void testUpdateAppWithEmptyBodyReturnsTheStoredApp() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(appDto(AppStatus.ENABLED));

        mockMvc.perform(patch("/v1/apps/1").contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(appFacade, never()).updateApp(
            anyLong(), any(), any(), any(), any(), any(), any());
        verify(appFacade, never()).updateAppStatus(anyLong(), any());
    }

    @Test
    void testDeleteAppReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/apps/1"))
            .andExpect(status().isNoContent());

        verify(appFacade).deleteApp(1L);
    }

    @Test
    void testRotateAppSecretKeyReturnsTheNewKey() throws Exception {
        when(appFacade.rotateSecretKey(1L)).thenReturn(new AppSecretKeyDTO(appDto(AppStatus.ENABLED), "rotated"));

        mockMvc.perform(post("/v1/apps/1/secret-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secretKey").value("rotated"));
    }

    @Test
    void testGetAppBindingsReturnsBindings() throws Exception {
        when(appFacade.getBindings(1L)).thenReturn(List.of(bindingDto()));

        mockMvc.perform(get("/v1/apps/1/connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].bindingKey").value("sales-slack"))
            .andExpect(jsonPath("$[0].actionNames[0]").value("sendMessage"))
            .andExpect(jsonPath("$[0].proxyEnabled").value(true));
    }

    @Test
    void testBindAppConnectionPassesTheInputAndDefaultsOmittedFields() throws Exception {
        when(appFacade.bindConnection(1L, "sales-slack", 5L, List.of("sendMessage"), List.of(), false, List.of()))
            .thenReturn(bindingDto());

        mockMvc
            .perform(
                put("/v1/apps/1/connections/sales-slack").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5,\"actionNames\":[\"sendMessage\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connectionName").value("Slack"));
    }

    @Test
    void testBindAppConnectionPassesProxySettings() throws Exception {
        when(appFacade.bindConnection(1L, "sales-slack", 5L, List.of(), List.of("tool"), true, List.of("GET")))
            .thenReturn(bindingDto());

        mockMvc
            .perform(
                put("/v1/apps/1/connections/sales-slack").contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"connectionId\":5,\"toolNames\":[\"tool\"],\"proxyEnabled\":true," +
                            "\"proxyAllowedMethods\":[\"GET\"]}"))
            .andExpect(status().isOk());
    }

    @Test
    void testUnbindAppConnectionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/apps/1/connections/sales-slack"))
            .andExpect(status().isNoContent());

        verify(appFacade).unbindConnection(1L, "sales-slack");
    }

    @Test
    void testAccessDeniedBecomesForbidden() throws Exception {
        when(appFacade.getApp(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    @ParameterizedTest
    @MethodSource("appErrors")
    void testAppErrorsMapToStatusAndCode(AppErrorType errorType, int expectedStatus, String expectedCode)
        throws Exception {

        when(appFacade.getApp(1L)).thenThrow(new ConfigurationException("boom", errorType));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().is(expectedStatus))
            .andExpect(jsonPath("$.code").value(expectedCode))
            .andExpect(jsonPath("$.message").value("boom"));
    }

    @Test
    void testToolExecutionFailureBecomesUnprocessable() throws Exception {
        when(appFacade.getApp(1L))
            .thenThrow(
                new ExecutionException(
                    "tool failed", Map.of("secret", "s3cr3t"), clusterElementDefinitionErrorType(104)));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("tool failed"))
            .andExpect(jsonPath("$.inputParameters").doesNotExist())
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testActionExecutionFailureBecomesUnprocessable() throws Exception {
        when(appFacade.getApp(1L))
            .thenThrow(
                new ExecutionException(
                    "perform failed", new IllegalStateException("upstream said no"), Map.of("secret", "s3cr3t"),
                    actionDefinitionErrorType(104)));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("perform failed"))
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testOtherConfigurationErrorBecomesUnprocessable() throws Exception {
        when(appFacade.getApp(1L))
            .thenThrow(new ConfigurationException("bad input", actionDefinitionErrorType(104)));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void testProviderExceptionBecomesUnprocessable() throws Exception {
        when(appFacade.getApp(1L)).thenThrow(new ProviderException("provider said no"));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"))
            .andExpect(jsonPath("$.message").value("provider said no"));
    }

    @Test
    void testExecutionFailureCausedByAProviderErrorBecomesProviderError() throws Exception {
        when(appFacade.getApp(1L))
            .thenThrow(
                new ExecutionException(
                    "perform failed", new IllegalStateException(new ProviderException("provider said no")),
                    Map.of("secret", "s3cr3t"), actionDefinitionErrorType(104)));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"))
            .andExpect(jsonPath("$.message").value("provider said no"))
            .andExpect(jsonPath("$.inputParameters").doesNotExist())
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testAppErrorCausedByAProviderErrorKeepsItsAppCode() throws Exception {
        when(appFacade.getApp(1L))
            .thenThrow(
                new ConfigurationException(
                    "disabled", new ProviderException("provider said no"), AppErrorType.APP_DISABLED));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_DISABLED"));
    }

    @Test
    void testIllegalArgumentExceptionBecomesBadRequest() throws Exception {
        when(appFacade.bindConnection(eq(1L), eq("sales-slack"), eq(5L), any(), any(), eq(true), any()))
            .thenThrow(new IllegalArgumentException("Unsupported proxy method: TRACE"));

        mockMvc
            .perform(
                put("/v1/apps/1/connections/sales-slack").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5,\"proxyEnabled\":true,\"proxyAllowedMethods\":[\"TRACE\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Unsupported proxy method: TRACE"));
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("routesWithRequestEnvironment")
    void testByIdRouteForAnAppOfAnotherEnvironmentIsNotFoundWithoutAnyOtherFacadeCall(
        Route route, @Nullable String requestEnvironment, Environment resolvedEnvironment) throws Exception {

        doThrow(new ConfigurationException("App 1 not found", AppErrorType.APP_NOT_FOUND)).when(appFacade)
            .requireAppInEnvironment(1L, resolvedEnvironment);

        mockMvc.perform(withEnvironment(route.request()
            .get(), requestEnvironment))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_NOT_FOUND"));

        verify(appFacade).requireAppInEnvironment(1L, resolvedEnvironment);
        verifyNoMoreInteractions(appFacade, appWorkflowFacade);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("routesWithRequestEnvironment")
    void testByIdRouteForAnAppOfAnotherEnvironmentIsNotFoundNotForbiddenWhenTheGuardedFacadeWouldDeny(
        Route route, @Nullable String requestEnvironment, Environment resolvedEnvironment) throws Exception {

        when(appFacade.getApp(1L)).thenThrow(new AccessDeniedException("Access Denied"));
        doThrow(new ConfigurationException("App 1 not found", AppErrorType.APP_NOT_FOUND)).when(appFacade)
            .requireAppInEnvironment(1L, resolvedEnvironment);

        mockMvc.perform(withEnvironment(route.request()
            .get(), requestEnvironment))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_NOT_FOUND"));

        verify(appFacade).requireAppInEnvironment(1L, resolvedEnvironment);
        verifyNoMoreInteractions(appFacade, appWorkflowFacade);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("routesWithRequestEnvironment")
    void testByIdRouteForAnAppOfTheRequestEnvironmentProceeds(
        Route route, @Nullable String requestEnvironment, Environment resolvedEnvironment) throws Exception {

        AppDTO app = givenApp();

        when(appFacade.updateApp(anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(app);
        when(appFacade.rotateSecretKey(1L)).thenReturn(new AppSecretKeyDTO(app, "secret"));
        when(appFacade.bindConnection(anyLong(), anyString(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(
                new AppBindingDTO(9L, 1L, "sales-slack", 5L, "Slack", "slack", 1, 2, List.of(), List.of(), false,
                    List.of()));

        mockMvc.perform(withEnvironment(route.request()
            .get(), requestEnvironment))
            .andExpect(status().is2xxSuccessful());

        InOrder inOrder = inOrder(appFacade);

        inOrder.verify(appFacade)
            .requireAppInEnvironment(1L, resolvedEnvironment);

        route.facadeCall()
            .accept(inOrder.verify(appFacade));

        verify(appFacade, times(route.guardedReads())).getApp(1L);
    }

    private static Stream<Arguments> routesWithRequestEnvironment() {
        return routes().flatMap(route -> Stream.of(
            Arguments.of(route, "DEVELOPMENT", Environment.DEVELOPMENT),
            Arguments.of(route, null, Environment.PRODUCTION)));
    }

    private static Stream<Route> routes() {
        return Stream.of(
            new Route("getApp", () -> get("/v1/apps/1"), facade -> facade.getApp(1L), 1),
            new Route(
                "updateApp",
                () -> patch("/v1/apps/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\"}"),
                facade -> facade.updateApp(anyLong(), any(), any(), any(), any(), any(), any()), 1),
            new Route("deleteApp", () -> delete("/v1/apps/1"), facade -> facade.deleteApp(anyLong()), 0),
            new Route(
                "rotateAppSecretKey", () -> post("/v1/apps/1/secret-key"),
                facade -> facade.rotateSecretKey(anyLong()), 0),
            new Route(
                "getAppBindableConnections", () -> get("/v1/apps/1/bindable-connections"),
                facade -> facade.getBindableConnections(anyLong(), any()), 0),
            new Route(
                "getAppBindings", () -> get("/v1/apps/1/connections"), facade -> facade.getBindings(anyLong()),
                0),
            new Route(
                "bindAppConnection",
                () -> put("/v1/apps/1/connections/sales-slack").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5}"),
                facade -> facade.bindConnection(
                    anyLong(), anyString(), anyLong(), any(), any(), anyBoolean(), any()),
                0),
            new Route(
                "unbindAppConnection", () -> delete("/v1/apps/1/connections/sales-slack"),
                facade -> facade.unbindConnection(anyLong(), anyString()), 0));
    }

    private static MockHttpServletRequestBuilder withEnvironment(
        MockHttpServletRequestBuilder request, @Nullable String environment) {

        return environment == null ? request : request.header("X-Environment", environment);
    }

    private static Stream<Arguments> appErrors() {
        return Stream.of(
            Arguments.of(AppErrorType.APP_NOT_FOUND, 404, "APP_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_CONNECTION_NOT_FOUND, 404, "APP_CONNECTION_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_DISABLED, 403, "APP_DISABLED"),
            Arguments.of(AppErrorType.APP_OPERATION_NOT_ALLOWED, 403, "APP_OPERATION_NOT_ALLOWED"),
            Arguments.of(AppErrorType.APP_CONNECTION_UNAVAILABLE, 409, "APP_CONNECTION_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_PROXY_UNSUPPORTED, 409, "APP_PROXY_UNSUPPORTED"),
            Arguments.of(AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS, 409, "APP_BINDING_KEY_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_EXECUTION_TIMEOUT, 504, "APP_EXECUTION_TIMEOUT"),
            Arguments.of(AppErrorType.APP_PROXY_REQUEST_TOO_LARGE, 413, "APP_PROXY_REQUEST_TOO_LARGE"),
            Arguments.of(AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE, 502, "APP_PROXY_RESPONSE_TOO_LARGE"),
            Arguments.of(AppErrorType.APP_BINDING_KEY_INVALID, 400, "APP_BINDING_KEY_INVALID"),
            Arguments.of(AppErrorType.APP_CONNECTION_NOT_BINDABLE, 400, "APP_CONNECTION_NOT_BINDABLE"),
            Arguments.of(AppErrorType.APP_NAME_ALREADY_EXISTS, 400, "APP_NAME_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_PROXY_PATH_REJECTED, 400, "APP_PROXY_PATH_REJECTED"),
            Arguments.of(AppErrorType.APP_VISIBILITY_UNSUPPORTED, 400, "APP_VISIBILITY_UNSUPPORTED"),
            Arguments.of(AppErrorType.APP_FIELD_TOO_LONG, 400, "APP_FIELD_TOO_LONG"),
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_FOUND, 404, "APP_WORKFLOW_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_UNAVAILABLE, 409, "APP_WORKFLOW_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_WORKFLOW_INPUT_INVALID, 400, "APP_WORKFLOW_INPUT_INVALID"),
            Arguments.of(AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND, 404, "APP_WORKFLOW_RUN_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_BINDABLE, 400, "APP_WORKFLOW_NOT_BINDABLE"),
            Arguments.of(AppErrorType.APP_PROXY_STORAGE_FAILED, 500, "APP_PROXY_STORAGE_FAILED"));
    }

    private static ErrorType actionDefinitionErrorType(int errorKey) {
        return new AbstractErrorType(ActionDefinition.class, errorKey) {};
    }

    private static ErrorType clusterElementDefinitionErrorType(int errorKey) {
        return new AbstractErrorType(ClusterElementDefinition.class, errorKey) {};
    }

    private static AppDTO appDto(AppStatus status) {
        return appDto(status, Environment.PRODUCTION);
    }

    private static AppDTO appDto(AppStatus status, Environment environment) {
        return new AppDTO(
            1L, "uuid-1", "Sales", "desc", 7L, environment, 3L, status, ResourceVisibility.WORKSPACE,
            AppHostingTarget.VERCEL, null, DEPLOYED_DATE, "hint1234", null, "admin", CREATED_DATE, "admin",
            CREATED_DATE, 0);
    }

    private static AppBindingDTO bindingDto() {
        return new AppBindingDTO(
            9L, 1L, "sales-slack", 5L, "Slack", "slack", 1, 2, List.of("sendMessage"), List.of(), true, List.of("GET"));
    }

    private AppDTO givenApp() {
        AppDTO app = appDto(AppStatus.ENABLED);

        when(appFacade.getApp(1L)).thenReturn(app);

        return app;
    }

    private record Route(
        String name, Supplier<MockHttpServletRequestBuilder> request, Consumer<AppFacade> facadeCall,
        int guardedReads) {

        @Override
        public String toString() {
            return name;
        }
    }
}
