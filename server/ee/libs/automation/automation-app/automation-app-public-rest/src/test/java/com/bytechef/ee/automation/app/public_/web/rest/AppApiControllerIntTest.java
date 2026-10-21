/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
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
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
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
        when(appFacade.getApps(7L)).thenReturn(
            List.of(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(Environment.DEVELOPMENT)))));

        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].instances.length()").value(1))
            .andExpect(jsonPath("$[0].instances[0].environment").value("DEVELOPMENT"));

        verify(appFacade).getApps(7L);
    }

    @ParameterizedTest(name = "X-Environment [{0}]")
    @ValueSource(strings = {
        "", "   "
    })
    void testGetAppsTreatsABlankEnvironmentHeaderAsAbsent(String requestEnvironment) throws Exception {
        when(appFacade.getApps(7L)).thenReturn(
            List.of(
                appDto(
                    AppStatus.ACTIVE,
                    List.of(appInstanceDto(Environment.DEVELOPMENT), appInstanceDto(Environment.PRODUCTION)))));

        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].instances.length()").value(1))
            .andExpect(jsonPath("$[0].instances[0].environment").value("PRODUCTION"));
    }

    @Test
    void testGetAppsRejectsAnUnknownEnvironmentHeader() throws Exception {
        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", "staging-x"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appFacade);
    }

    @Test
    void testGetAppsListsEveryAppWithOnlyTheInstancesOfTheRequestEnvironment() throws Exception {
        AppInstanceDTO productionAppInstance = appInstanceDto(Environment.PRODUCTION);
        AppInstanceDTO stagingAppInstance = appInstanceDto(Environment.STAGING);

        when(appFacade.getApps(7L)).thenReturn(
            List.of(
                appDto(AppStatus.ACTIVE, List.of(productionAppInstance, stagingAppInstance)),
                appDto(AppStatus.DRAFT, List.of())));

        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", "STAGING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Sales"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[0].visibility").value("WORKSPACE"))
            .andExpect(jsonPath("$[0].instances.length()").value(1))
            .andExpect(jsonPath("$[0].instances[0].environment").value("STAGING"))
            .andExpect(jsonPath("$[0].instances[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[0].instances[0].hostingTarget").value("VERCEL"))
            .andExpect(jsonPath("$[0].instances[0].appId").value(1))
            .andExpect(jsonPath("$[1].status").value("DRAFT"))
            .andExpect(jsonPath("$[1].instances.length()").value(0));
    }

    @Test
    void testGetAppReturnsTheAppWithForeignInstancesOfTheRequestEnvironment() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(
            appDto(
                AppStatus.ACTIVE, List.of(appInstanceDto(Environment.PRODUCTION)),
                List.of(
                    foreignAppInstanceDto(Environment.PRODUCTION), foreignAppInstanceDto(Environment.STAGING))));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uuid").value("uuid-1"))
            .andExpect(jsonPath("$.hasIcon").value(false))
            .andExpect(jsonPath("$.instances[0].appHasIcon").value(false))
            .andExpect(jsonPath("$.workspaceId").value(7))
            .andExpect(jsonPath("$.instances.length()").value(1))
            .andExpect(jsonPath("$.foreignInstances.length()").value(1))
            .andExpect(jsonPath("$.foreignInstances[0].workspaceId").value(8))
            .andExpect(jsonPath("$.foreignInstances[0].environment").value("PRODUCTION"))
            .andExpect(jsonPath("$.foreignInstances[0].status").value("ACTIVE"));
    }

    @Test
    void testGetAppTellsWhetherTheAppHasAnIconWithoutTheStoredFile() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(
            new AppDTO(
                1L, "uuid-1", "Sales", "desc", 7L, 3L, null, true, AppStatus.ACTIVE, ResourceVisibility.WORKSPACE,
                false, List.of(), List.of(), "admin", CREATED_DATE, "admin", CREATED_DATE, 0));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasIcon").value(true))
            .andExpect(jsonPath("$.icon").doesNotExist());
    }

    @Test
    void testGetAppWhoseInstancesAreAllHiddenReturnsTheAppWithNoInstances() throws Exception {
        when(appFacade.getApp(1L)).thenReturn(appDto(AppStatus.ACTIVE, List.of()));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.instances.length()").value(0));
    }

    @Test
    void testCreateAppReturnsCreatedWithTheFirstInstanceAndItsSecretKey() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.DEVELOPMENT, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                null, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(Environment.DEVELOPMENT), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"description\":\"desc\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.secretKey").value("secret"))
            .andExpect(jsonPath("$.appInstance.appId").value(1))
            .andExpect(jsonPath("$.appInstance.environment").value("DEVELOPMENT"))
            .andExpect(jsonPath("$.appInstance.secretKeyHint").value("hint1234"));

        verify(appFacade).createApp(
            7L, Environment.DEVELOPMENT, "Sales", "desc", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            null, false);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testCreateAppPassesTheRequestedVisibility() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.DEVELOPMENT, "Sales", null, AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE,
                null, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(Environment.DEVELOPMENT), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"SELF_HOSTED\",\"visibility\":\"PRIVATE\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void testCreateAppPassesTheRequestedSignInMode() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.DEVELOPMENT, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.NONE, null, false))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(Environment.DEVELOPMENT), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"VERCEL\",\"userAuthMode\":\"NONE\"}"))
            .andExpect(status().isCreated());

        verify(appFacade).createApp(
            7L, Environment.DEVELOPMENT, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, null, false);
    }

    @Test
    void testCreateAppPassesTheCategoryAndPortalVisibility() throws Exception {
        when(
            appFacade.createApp(
                7L, Environment.DEVELOPMENT, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                null, new AppCategoryDTO(null, "Finance"), true))
                    .thenReturn(new AppSecretKeyDTO(appInstanceDto(Environment.DEVELOPMENT), "secret"));

        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Sales\",\"hostingTarget\":\"VERCEL\",\"category\":{\"name\":\"Finance\"}," +
                            "\"portalDiscoverable\":true}"))
            .andExpect(status().isCreated());

        verify(appFacade).createApp(
            7L, Environment.DEVELOPMENT, "Sales", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            new AppCategoryDTO(null, "Finance"), true);
    }

    @Test
    void testCreateAppWithANameLongerThanTheColumnIsRejectedBeforeTheFacade() throws Exception {
        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + "x".repeat(257) + "\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value(containsString("name")));

        verifyNoInteractions(appFacade);
    }

    @Test
    void testCreateAppWithoutAnEnvironmentHeaderIsRefusedAsProduction() throws Exception {
        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));

        verifyNoInteractions(appFacade);
    }

    @Test
    void testUpdateAppMakesOneFacadeCall() throws Exception {
        when(appFacade.updateApp(1L, "Renamed", "", null, null)).thenReturn(appDto(AppStatus.ACTIVE));

        mockMvc
            .perform(
                patch("/v1/apps/1").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\",\"description\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(appFacade).updateApp(1L, "Renamed", "", null, null);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testUpdateAppPassesTheCategoryAndPortalVisibility() throws Exception {
        when(appFacade.updateApp(1L, null, null, new AppCategoryDTO(42L, null), true))
            .thenReturn(appDto(AppStatus.ACTIVE));

        mockMvc
            .perform(
                patch("/v1/apps/1").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"category\":{\"id\":42},\"portalDiscoverable\":true}"))
            .andExpect(status().isOk());

        verify(appFacade).updateApp(1L, null, null, new AppCategoryDTO(42L, null), true);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testUpdateAppStatusMakesOneFacadeCall() throws Exception {
        when(appFacade.updateAppStatus(1L, AppStatus.SUSPENDED)).thenReturn(appDto(AppStatus.SUSPENDED));

        mockMvc
            .perform(
                put("/v1/apps/1/status").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"SUSPENDED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(appFacade).updateAppStatus(1L, AppStatus.SUSPENDED);
        verifyNoMoreInteractions(appFacade);
    }

    @Test
    void testDeleteAppReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/apps/1").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNoContent());

        verify(appFacade).deleteApp(1L);
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @MethodSource("nonDevelopmentEnvironments")
    void testCreateAppOutsideDevelopmentIs403(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"VERCEL\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("mutationsOutsideDevelopment")
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

    @ParameterizedTest(name = "X-Environment {0}")
    @MethodSource("nonDevelopmentEnvironments")
    void testReadsOutsideDevelopmentAreAllowed(String requestEnvironment) throws Exception {
        Environment environment = Environment.valueOf(requestEnvironment);

        when(appFacade.getApps(7L)).thenReturn(List.of(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(environment)))));
        when(appFacade.getApp(1L)).thenReturn(appDto(AppStatus.ACTIVE, List.of(appInstanceDto(environment))));

        mockMvc.perform(get("/v1/apps").param("workspaceId", "7")
            .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].instances[0].environment").value(requestEnvironment));

        mockMvc.perform(get("/v1/apps/1").header("X-Environment", requestEnvironment))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instances[0].environment").value(requestEnvironment));
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
        when(appFacade.updateApp(1L, "Renamed", null, null, null))
            .thenThrow(new IllegalArgumentException("Unsupported value"));

        mockMvc
            .perform(
                patch("/v1/apps/1").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Unsupported value"));
    }

    private static Stream<String> nonDevelopmentEnvironments() {
        return Stream.of("STAGING", "PRODUCTION");
    }

    private static Stream<Arguments> mutationsOutsideDevelopment() {
        return Stream.of(
            new Route(
                "updateApp",
                environment -> patch("/v1/apps/1").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Renamed\"}")),
            new Route(
                "updateAppStatus",
                environment -> put("/v1/apps/1/status").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"SUSPENDED\"}")),
            new Route("deleteApp", environment -> delete("/v1/apps/1").header("X-Environment", environment)),
            new Route(
                "createApp",
                environment -> post("/v1/apps").param("workspaceId", "7")
                    .header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Sales\",\"hostingTarget\":\"VERCEL\"}")))
            .flatMap(route -> nonDevelopmentEnvironments().map(environment -> Arguments.of(route, environment)));
    }

    private static Stream<Arguments> appErrors() {
        return Stream.of(
            Arguments.of(AppErrorType.APP_NOT_FOUND, 404, "APP_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_CONNECTION_NOT_FOUND, 404, "APP_CONNECTION_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_DISABLED, 403, "APP_DISABLED"),
            Arguments.of(AppErrorType.APP_OPERATION_NOT_ALLOWED, 403, "APP_OPERATION_NOT_ALLOWED"),
            Arguments.of(AppErrorType.APP_OPERATION_DEVELOPMENT_ONLY, 403, "APP_OPERATION_DEVELOPMENT_ONLY"),
            Arguments.of(AppErrorType.APP_CONNECTION_UNAVAILABLE, 409, "APP_CONNECTION_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_PROXY_UNSUPPORTED, 409, "APP_PROXY_UNSUPPORTED"),
            Arguments.of(AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS, 409, "APP_BINDING_KEY_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_EXECUTION_TIMEOUT, 504, "APP_EXECUTION_TIMEOUT"),
            Arguments.of(AppErrorType.APP_PROXY_REQUEST_TOO_LARGE, 413, "APP_PROXY_REQUEST_TOO_LARGE"),
            Arguments.of(AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE, 502, "APP_PROXY_RESPONSE_TOO_LARGE"),
            Arguments.of(AppErrorType.APP_BINDING_KEY_INVALID, 400, "APP_BINDING_KEY_INVALID"),
            Arguments.of(AppErrorType.APP_CONNECTION_NOT_BINDABLE, 400, "APP_CONNECTION_NOT_BINDABLE"),
            Arguments.of(AppErrorType.APP_NAME_ALREADY_EXISTS, 409, "APP_NAME_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_INSTANCE_URL_INVALID, 400, "APP_INSTANCE_URL_INVALID"),
            Arguments.of(AppErrorType.APP_PROXY_PATH_REJECTED, 400, "APP_PROXY_PATH_REJECTED"),
            Arguments.of(AppErrorType.APP_VISIBILITY_UNSUPPORTED, 400, "APP_VISIBILITY_UNSUPPORTED"),
            Arguments.of(AppErrorType.APP_FIELD_TOO_LONG, 400, "APP_FIELD_TOO_LONG"),
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_FOUND, 404, "APP_WORKFLOW_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_UNAVAILABLE, 409, "APP_WORKFLOW_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_WORKFLOW_INPUT_INVALID, 400, "APP_WORKFLOW_INPUT_INVALID"),
            Arguments.of(AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND, 404, "APP_WORKFLOW_RUN_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_WORKFLOW_NOT_BINDABLE, 400, "APP_WORKFLOW_NOT_BINDABLE"),
            Arguments.of(AppErrorType.APP_PROXY_STORAGE_FAILED, 500, "APP_PROXY_STORAGE_FAILED"),
            Arguments.of(AppErrorType.APP_INSTANCE_NOT_FOUND, 404, "APP_INSTANCE_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS, 409, "APP_INSTANCE_NAME_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_INSTANCE_TERMINATED, 409, "APP_INSTANCE_TERMINATED"),
            Arguments.of(
                AppErrorType.APP_INSTANCE_CONFIGURATION_INVALID, 400, "APP_INSTANCE_CONFIGURATION_INVALID"),
            Arguments.of(
                AppErrorType.APP_INSTANCE_CONFIGURATION_TOO_LARGE, 413, "APP_INSTANCE_CONFIGURATION_TOO_LARGE"),
            Arguments.of(AppErrorType.APP_ICON_INVALID, 400, "APP_ICON_INVALID"),
            Arguments.of(AppErrorType.APP_USER_AUTH_UNAVAILABLE, 409, "APP_USER_AUTH_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_USER_REQUIRED, 401, "APP_USER_REQUIRED"),
            Arguments.of(AppErrorType.APP_USER_TOKEN_INVALID, 401, "APP_USER_TOKEN_INVALID"),
            Arguments.of(AppErrorType.APP_USER_FORBIDDEN, 403, "APP_USER_FORBIDDEN"),
            Arguments.of(AppErrorType.APP_VERSION_NOT_FOUND, 404, "APP_VERSION_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_NOT_FOUND, 404, "APP_DEPLOYMENT_NOT_FOUND"),
            Arguments.of(AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS, 409, "APP_VERSION_NAME_ALREADY_EXISTS"),
            Arguments.of(AppErrorType.APP_VERSION_COMMIT_MISMATCH, 409, "APP_VERSION_COMMIT_MISMATCH"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_TERMINAL, 409, "APP_DEPLOYMENT_TERMINAL"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_URL_INVALID, 400, "APP_DEPLOYMENT_URL_INVALID"),
            Arguments.of(AppErrorType.APP_LIST_LIMIT_INVALID, 400, "APP_LIST_LIMIT_INVALID"),
            Arguments.of(
                AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE, 409, "APP_DEPLOYMENT_PROVIDER_UNAVAILABLE"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS, 409, "APP_DEPLOYMENT_IN_PROGRESS"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_PROVIDER_FAILED, 502, "APP_DEPLOYMENT_PROVIDER_FAILED"),
            Arguments.of(AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID, 400, "APP_DEPLOYMENT_SETTINGS_INVALID"),
            Arguments.of(AppErrorType.APP_CATEGORY_INVALID, 400, "APP_CATEGORY_INVALID"));
    }

    private static ErrorType actionDefinitionErrorType(int errorKey) {
        return new AbstractErrorType(ActionDefinition.class, errorKey) {};
    }

    private static ErrorType clusterElementDefinitionErrorType(int errorKey) {
        return new AbstractErrorType(ClusterElementDefinition.class, errorKey) {};
    }

    private static AppDTO appDto(AppStatus status) {
        return appDto(status, List.of(appInstanceDto(Environment.PRODUCTION)));
    }

    private static AppDTO appDto(AppStatus status, List<AppInstanceDTO> appInstances) {
        return appDto(status, appInstances, List.of());
    }

    private static AppDTO appDto(
        AppStatus status, List<AppInstanceDTO> appInstances, List<ForeignAppInstanceDTO> foreignAppInstances) {

        return new AppDTO(
            1L, "uuid-1", "Sales", "desc", 7L, 3L, null, false, status, ResourceVisibility.WORKSPACE, false,
            appInstances, foreignAppInstances, "admin", CREATED_DATE, "admin", CREATED_DATE, 0);
    }

    private static AppInstanceDTO appInstanceDto(Environment environment) {
        return new AppInstanceDTO(
            APP_INSTANCE_ID + environment.ordinal(), "instance-uuid-" + environment.ordinal(),
            new AppInstanceDTO.AppSummary(1L, "Sales", "desc", null, false, AppStatus.ACTIVE), 7L, environment,
            environment.name(), 3L, AppInstanceStatus.ACTIVE, ResourceVisibility.WORKSPACE, AppHostingTarget.VERCEL,
            null, "hint1234", null, AppUserAuthMode.NONE, Map.of(), null, null, Map.of(), "admin", CREATED_DATE,
            "admin", CREATED_DATE, 0);
    }

    private static ForeignAppInstanceDTO foreignAppInstanceDto(Environment environment) {
        return new ForeignAppInstanceDTO(
            40L + environment.ordinal(), "Hosted", 8L, environment, AppInstanceStatus.ACTIVE, null);
    }

    private record Route(String name, Function<String, MockHttpServletRequestBuilder> requestFactory) {

        MockHttpServletRequestBuilder request(String environment) {
            return requestFactory.apply(environment);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
