/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
@WebMvcTest(AppInstanceApiController.class)
@WithMockUser
class AppInstanceApiControllerIntTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final long APP_INSTANCE_ID = 30L;

    @MockitoBean
    private AppApiCollectionFacade appApiCollectionFacade;

    @MockitoBean
    private AppFacade appFacade;

    @MockitoBean
    private AppInstanceFacade appInstanceFacade;

    @MockitoBean
    private AppWorkflowFacade appWorkflowFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        AppInstanceDTO appInstance = appInstanceDto(Environment.DEVELOPMENT);

        when(appInstanceFacade.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceFacade.updateAppInstance(anyLong(), any(), any(), any(), any())).thenReturn(appInstance);
        when(appInstanceFacade.updateAppInstanceStatus(anyLong(), any())).thenReturn(appInstance);
        when(appInstanceFacade.updateAppInstanceUserAuth(anyLong(), any())).thenReturn(appInstance);
        when(appInstanceFacade.updateAppInstanceDeploymentSettings(anyLong(), any(), any())).thenReturn(appInstance);
        when(appInstanceFacade.rotateSecretKey(APP_INSTANCE_ID))
            .thenReturn(new AppSecretKeyDTO(appInstance, "rotated"));
        when(appInstanceFacade.bindConnection(anyLong(), anyString(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(bindingDto());
    }

    @Test
    void testGetAppInstancesListsOnlyTheInstancesOfTheRequestEnvironment() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(
            appDto(List.of(appInstanceDto(Environment.DEVELOPMENT), appInstanceDto(Environment.STAGING))));

        mockMvc.perform(get("/v1/apps/1/instances").header("X-Environment", "STAGING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].environment").value("STAGING"))
            .andExpect(jsonPath("$[0].appId").value(1))
            .andExpect(jsonPath("$[0].appName").value("Sales"))
            .andExpect(jsonPath("$[0].appStatus").value("ACTIVE"))
            .andExpect(jsonPath("$[0].appHasIcon").value(false));
    }

    @Test
    void testGetAppInstancesOfAnAppWhoseInstancesAreAllHiddenIsEmpty() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(appDto(List.of()));

        mockMvc.perform(get("/v1/apps/1/instances").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAppInstanceReturnsTheInstance() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(APP_INSTANCE_ID))
            .andExpect(jsonPath("$.uuid").value("instance-uuid-30"))
            .andExpect(jsonPath("$.workspaceId").value(7))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.hostingTarget").value("VERCEL"))
            .andExpect(jsonPath("$.userAuthMode").value("NONE"))
            .andExpect(jsonPath("$.secretKeyHint").value("hint1234"))
            .andExpect(jsonPath("$.deploymentConnectionId").value(9))
            .andExpect(jsonPath("$.deploymentSettings.projectId").value("prj_1"));
    }

    @Test
    void testUpdateAppInstanceDeploymentSettingsPassesTheConnectionAndTheSettings() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/deployment-settings").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":9,\"settings\":{\"projectId\":\"prj_1\",\"repository\":\"acme/crm\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deploymentConnectionId").value(9));

        verify(appInstanceFacade).updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 9L, Map.of("projectId", "prj_1", "repository", "acme/crm"));
    }

    @Test
    void testUpdateAppInstanceDeploymentSettingsWithAnEmptyBodyAsksTheFacadeToClear() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/deployment-settings").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, Map.of());
    }

    @Test
    void testUpdateAppInstanceDeploymentSettingsTheProviderRefusesIs400() throws Exception {
        when(appInstanceFacade.updateAppInstanceDeploymentSettings(anyLong(), any(), any()))
            .thenThrow(
                new ConfigurationException(
                    "The repository must be owner/name", AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID));

        mockMvc
            .perform(
                put("/v1/app-instances/30/deployment-settings").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":9,\"settings\":{\"repository\":\"crm\"}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_SETTINGS_INVALID"));
    }

    @Test
    void testCreateAppInstanceReturnsCreatedWithTheSecretKey() throws Exception {
        when(
            appInstanceFacade.createAppInstance(
                1L, 8L, Environment.DEVELOPMENT, "Hosted", AppHostingTarget.SELF_HOSTED, ResourceVisibility.WORKSPACE,
                null))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(Environment.DEVELOPMENT), "secret"));

        mockMvc
            .perform(
                post("/v1/apps/1/instances").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"workspaceId\":8,\"name\":\"Hosted\",\"hostingTarget\":\"SELF_HOSTED\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.secretKey").value("secret"))
            .andExpect(jsonPath("$.appInstance.id").value(APP_INSTANCE_ID));

        verify(appInstanceFacade).createAppInstance(
            1L, 8L, Environment.DEVELOPMENT, "Hosted", AppHostingTarget.SELF_HOSTED, ResourceVisibility.WORKSPACE,
            null);
        verifyNoMoreInteractions(appInstanceFacade);
        verifyNoInteractions(appFacade);
    }

    @Test
    void testUpdateAppInstanceMakesOneFacadeCall() throws Exception {
        mockMvc
            .perform(
                patch("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\",\"url\":\"https://example.com\"}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, "Renamed", null, "https://example.com", null);
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testGetAppInstanceReturnsTheConfiguration() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configuration.country").value("HR"))
            .andExpect(jsonPath("$.configuration.limits.seats").value(5));
    }

    @Test
    void testUpdateAppInstanceWithOnlyAConfigurationMakesOneFacadeCall() throws Exception {
        mockMvc
            .perform(
                patch("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"configuration\":{\"country\":\"DE\"}}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, null, null, null, Map.of("country", "DE"));
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testUpdateAppInstanceWithFieldsAndAConfigurationMakesOneFacadeCall() throws Exception {
        mockMvc
            .perform(
                patch("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\",\"configuration\":{\"country\":\"DE\"}}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, "Renamed", null, null, Map.of("country", "DE"));
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testUpdateAppInstanceWhoseFieldUpdateFailsAnswersTheError() throws Exception {
        when(appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Taken", null, null, Map.of("country", "DE")))
            .thenThrow(
                new ConfigurationException("name taken", AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS));

        mockMvc
            .perform(
                patch("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Taken\",\"configuration\":{\"country\":\"DE\"}}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NAME_ALREADY_EXISTS"));

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, "Taken", null, null, Map.of("country", "DE"));
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testUpdateAppInstanceWithANullConfigurationKeepsIt() throws Exception {
        mockMvc
            .perform(
                patch("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"configuration\":null}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstance(APP_INSTANCE_ID, null, null, null, null);
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testUpdateAppInstanceStatusMakesOneFacadeCall() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/status").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"SUSPENDED\"}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED);
    }

    @Test
    void testUpdateAppInstanceUserAuthMakesOneFacadeCall() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/user-auth").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userAuthMode\":\"REQUIRED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(APP_INSTANCE_ID));

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verify(appInstanceFacade).updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED);
        verifyNoMoreInteractions(appInstanceFacade);
    }

    @Test
    void testDeleteAppInstanceReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/app-instances/30").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNoContent());

        verify(appInstanceFacade).deleteAppInstance(APP_INSTANCE_ID);
    }

    @Test
    void testRotateAppInstanceSecretKeyReturnsTheNewKey() throws Exception {
        mockMvc.perform(post("/v1/app-instances/30/secret-key").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secretKey").value("rotated"))
            .andExpect(jsonPath("$.appInstance.id").value(APP_INSTANCE_ID))
            .andExpect(jsonPath("$.appInstance.secretKeyHint").value("hint1234"));
    }

    @Test
    void testGetAppInstanceBindableConnectionsPassesTheComponentFilterAndReturnsUsable() throws Exception {
        when(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, "slack")).thenReturn(
            List.of(
                new AppBindableConnectionDTO(101L, "Slack A", "slack", 1, 2, true),
                new AppBindableConnectionDTO(102L, "Slack B", "slack", 1, 2, false)));

        mockMvc.perform(get("/v1/app-instances/30/bindable-connections").param("componentName", "slack"))
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
    void testGetAppInstanceBindableConnectionsWithoutAFilterPassesNull() throws Exception {
        when(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/app-instances/30/bindable-connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(appInstanceFacade).getBindableConnections(APP_INSTANCE_ID, null);
    }

    @Test
    void testGetAppInstanceBindableConnectionsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null))
            .thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/app-instances/30/bindable-connections"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testGetAppInstanceBindingsReturnsBindings() throws Exception {
        when(appInstanceFacade.getBindings(APP_INSTANCE_ID)).thenReturn(List.of(bindingDto()));

        mockMvc.perform(get("/v1/app-instances/30/connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].appInstanceId").value(APP_INSTANCE_ID))
            .andExpect(jsonPath("$[0].bindingKey").value("sales-slack"))
            .andExpect(jsonPath("$[0].actionNames[0]").value("sendMessage"))
            .andExpect(jsonPath("$[0].proxyEnabled").value(true));
    }

    @Test
    void testBindAppInstanceConnectionPassesTheInputAndDefaultsOmittedFields() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/connections/sales-slack").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5,\"actionNames\":[\"sendMessage\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connectionName").value("Slack"));

        verify(appInstanceFacade).bindConnection(
            APP_INSTANCE_ID, "sales-slack", 5L, List.of("sendMessage"), List.of(), false, List.of());
    }

    @Test
    void testBindAppInstanceConnectionPassesProxySettings() throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/connections/sales-slack").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"connectionId\":5,\"toolNames\":[\"tool\"],\"proxyEnabled\":true," +
                            "\"proxyAllowedMethods\":[\"GET\"]}"))
            .andExpect(status().isOk());

        verify(appInstanceFacade).bindConnection(
            APP_INSTANCE_ID, "sales-slack", 5L, List.of(), List.of("tool"), true, List.of("GET"));
    }

    @Test
    void testBindAppInstanceConnectionIllegalArgumentBecomesBadRequest() throws Exception {
        when(
            appInstanceFacade.bindConnection(
                eq(APP_INSTANCE_ID), eq("sales-slack"), eq(5L), any(), any(), eq(true), any()))
                    .thenThrow(new IllegalArgumentException("Unsupported proxy method: TRACE"));

        mockMvc
            .perform(
                put("/v1/app-instances/30/connections/sales-slack").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5,\"proxyEnabled\":true,\"proxyAllowedMethods\":[\"TRACE\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Unsupported proxy method: TRACE"));
    }

    @Test
    void testUnbindAppInstanceConnectionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/app-instances/30/connections/sales-slack").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNoContent());

        verify(appInstanceFacade).unbindConnection(APP_INSTANCE_ID, "sales-slack");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("byIdRoutes")
    void testInstanceOfAnotherEnvironmentIs404(Route route) throws Exception {
        doThrow(new ConfigurationException("App instance 30 not found", AppErrorType.APP_INSTANCE_NOT_FOUND))
            .when(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);

        mockMvc.perform(route.request("DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NOT_FOUND"));

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verifyNoMoreInteractions(appInstanceFacade);
        verifyNoInteractions(appFacade);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("byIdRoutes")
    void testInstanceOfAnotherEnvironmentIs404NotForbiddenWhenTheGuardedFacadeWouldDeny(Route route)
        throws Exception {

        when(appInstanceFacade.getAppInstance(APP_INSTANCE_ID)).thenThrow(new AccessDeniedException("Access Denied"));
        doThrow(new ConfigurationException("App instance 30 not found", AppErrorType.APP_INSTANCE_NOT_FOUND))
            .when(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);

        mockMvc.perform(route.request("DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NOT_FOUND"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("byIdRoutes")
    void testByIdRouteChecksTheEnvironmentBeforeTheFacadeCall(Route route) throws Exception {
        mockMvc.perform(route.request("DEVELOPMENT"))
            .andExpect(status().is2xxSuccessful());

        InOrder inOrder = inOrder(appInstanceFacade);

        inOrder.verify(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);

        Consumer<AppInstanceFacade> facadeCall = Objects.requireNonNull(route.facadeCall());

        facadeCall.accept(inOrder.verify(appInstanceFacade));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @MethodSource("nonDevelopmentEnvironments")
    void testCreateInstanceOutsideDevelopmentIs403(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                post("/v1/apps/1/instances").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"workspaceId\":8,\"name\":\"Hosted\",\"hostingTarget\":\"SELF_HOSTED\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @MethodSource("nonDevelopmentEnvironments")
    void testRotateKeyOutsideDevelopmentIs403(String requestEnvironment) throws Exception {
        mockMvc.perform(post("/v1/app-instances/30/secret-key").header("X-Environment", requestEnvironment))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @MethodSource("nonDevelopmentEnvironments")
    void testBindOutsideDevelopmentIs403(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                put("/v1/app-instances/30/connections/sales-slack").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));

        mockMvc
            .perform(
                delete("/v1/app-instances/30/connections/sales-slack").header("X-Environment", requestEnvironment))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("updateAndDeleteOutsideDevelopment")
    void testUpdateAndDeleteOutsideDevelopmentAre403(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("mutationsOutsideDevelopment")
    void testRefusalHappensBeforeTheFacadeIsCalled(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isForbidden());

        verifyNoInteractions(appFacade, appInstanceFacade);
    }

    @Test
    void testMutationWithoutAnEnvironmentHeaderIsRefusedAsProduction() throws Exception {
        mockMvc.perform(delete("/v1/app-instances/30"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));

        verifyNoInteractions(appInstanceFacade);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("readsOutsideDevelopment")
    void testReadsOutsideDevelopmentAreAllowed(Route route, String requestEnvironment) throws Exception {
        when(appFacade.getApp(1L)).thenReturn(appDto(List.of(appInstanceDto(Environment.valueOf(requestEnvironment)))));

        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isOk());

        if (route.facadeCall() != null) {
            verify(appInstanceFacade).requireAppInstanceInEnvironment(
                APP_INSTANCE_ID, Environment.valueOf(requestEnvironment));
        }
    }

    private static Stream<String> nonDevelopmentEnvironments() {
        return Stream.of("STAGING", "PRODUCTION");
    }

    private static Stream<Route> reads() {
        return Stream.of(
            new Route(
                "getAppInstances", environment -> get("/v1/apps/1/instances").header("X-Environment", environment),
                null),
            new Route(
                "getAppInstance", environment -> get("/v1/app-instances/30").header("X-Environment", environment),
                facade -> facade.getAppInstance(APP_INSTANCE_ID)),
            new Route(
                "getAppInstanceBindings",
                environment -> get("/v1/app-instances/30/connections").header("X-Environment", environment),
                facade -> facade.getBindings(APP_INSTANCE_ID)),
            new Route(
                "getAppInstanceBindableConnections",
                environment -> get("/v1/app-instances/30/bindable-connections").header("X-Environment", environment),
                facade -> facade.getBindableConnections(eq(APP_INSTANCE_ID), any())));
    }

    private static Stream<Route> updatesAndDeletes() {
        return Stream.of(
            new Route(
                "updateAppInstance",
                environment -> patch("/v1/app-instances/30").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\"}"),
                facade -> facade.updateAppInstance(eq(APP_INSTANCE_ID), any(), any(), any(), any())),
            new Route(
                "updateAppInstanceConfiguration",
                environment -> patch("/v1/app-instances/30").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"configuration\":{\"country\":\"DE\"}}"),
                facade -> facade.updateAppInstance(eq(APP_INSTANCE_ID), any(), any(), any(), any())),
            new Route(
                "updateAppInstanceStatus",
                environment -> put("/v1/app-instances/30/status").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"SUSPENDED\"}"),
                facade -> facade.updateAppInstanceStatus(eq(APP_INSTANCE_ID), any())),
            new Route(
                "updateAppInstanceUserAuth",
                environment -> put("/v1/app-instances/30/user-auth").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userAuthMode\":\"REQUIRED\"}"),
                facade -> facade.updateAppInstanceUserAuth(eq(APP_INSTANCE_ID), any())),
            new Route(
                "updateAppInstanceDeploymentSettings",
                environment -> put("/v1/app-instances/30/deployment-settings").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":9,\"settings\":{\"projectId\":\"prj_1\"}}"),
                facade -> facade.updateAppInstanceDeploymentSettings(eq(APP_INSTANCE_ID), any(), any())),
            new Route(
                "deleteAppInstance", environment -> delete("/v1/app-instances/30").header("X-Environment", environment),
                facade -> facade.deleteAppInstance(APP_INSTANCE_ID)));
    }

    private static Stream<Route> otherMutations() {
        return Stream.of(
            new Route(
                "rotateAppInstanceSecretKey",
                environment -> post("/v1/app-instances/30/secret-key").header("X-Environment", environment),
                facade -> facade.rotateSecretKey(APP_INSTANCE_ID)),
            new Route(
                "bindAppInstanceConnection",
                environment -> put("/v1/app-instances/30/connections/sales-slack").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"connectionId\":5}"),
                facade -> facade.bindConnection(
                    eq(APP_INSTANCE_ID), anyString(), anyLong(), any(), any(), anyBoolean(), any())),
            new Route(
                "unbindAppInstanceConnection",
                environment -> delete("/v1/app-instances/30/connections/sales-slack")
                    .header("X-Environment", environment),
                facade -> facade.unbindConnection(eq(APP_INSTANCE_ID), anyString())));
    }

    private static Stream<Route> byIdRoutes() {
        return Stream.of(reads().filter(route -> route.facadeCall() != null), updatesAndDeletes(), otherMutations())
            .flatMap(routes -> routes);
    }

    private static Stream<Arguments> readsOutsideDevelopment() {
        return withNonDevelopmentEnvironments(reads());
    }

    private static Stream<Arguments> updateAndDeleteOutsideDevelopment() {
        return withNonDevelopmentEnvironments(updatesAndDeletes());
    }

    private static Stream<Arguments> mutationsOutsideDevelopment() {
        Route createAppInstance = new Route(
            "createAppInstance",
            environment -> post("/v1/apps/1/instances").header("X-Environment", environment)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":8,\"name\":\"Hosted\",\"hostingTarget\":\"SELF_HOSTED\"}"),
            null);

        return withNonDevelopmentEnvironments(
            Stream.concat(Stream.of(createAppInstance), Stream.concat(updatesAndDeletes(), otherMutations())));
    }

    private static Stream<Arguments> withNonDevelopmentEnvironments(Stream<Route> routes) {
        return routes.flatMap(
            route -> nonDevelopmentEnvironments().map(environment -> Arguments.of(route, environment)));
    }

    private static AppDTO appDto(List<AppInstanceDTO> appInstances) {
        return new AppDTO(
            1L, "uuid-1", "Sales", "desc", 7L, 3L, null, List.of(), false, AppStatus.ACTIVE,
            ResourceVisibility.WORKSPACE, false,
            appInstances, List.of(), "admin", CREATED_DATE, "admin", CREATED_DATE, 0);
    }

    private static AppInstanceDTO appInstanceDto(Environment environment) {
        return new AppInstanceDTO(
            APP_INSTANCE_ID + environment.ordinal(), "instance-uuid-" + (APP_INSTANCE_ID + environment.ordinal()),
            new AppInstanceDTO.AppSummary(1L, "Sales", "desc", null, false, AppStatus.ACTIVE), 7L, environment,
            environment.name(), 3L, AppInstanceStatus.ACTIVE, ResourceVisibility.WORKSPACE, AppHostingTarget.VERCEL,
            null, "hint1234", null, AppUserAuthMode.NONE, Map.of("country", "HR", "limits", Map.of("seats", 5)), null,
            9L, Map.of("projectId", "prj_1"), "admin", CREATED_DATE, "admin", CREATED_DATE, 0);
    }

    private static AppBindingDTO bindingDto() {
        return new AppBindingDTO(
            9L, APP_INSTANCE_ID, "sales-slack", 5L, "Slack", "slack", 1, 2, List.of("sendMessage"), List.of(), true,
            List.of("GET"));
    }

    private record Route(
        String name, Function<String, MockHttpServletRequestBuilder> requestFactory,
        @Nullable Consumer<AppInstanceFacade> facadeCall) {

        MockHttpServletRequestBuilder request(String environment) {
            return requestFactory.apply(environment);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
