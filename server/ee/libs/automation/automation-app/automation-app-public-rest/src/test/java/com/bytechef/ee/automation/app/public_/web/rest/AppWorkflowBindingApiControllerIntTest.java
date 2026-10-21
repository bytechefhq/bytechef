/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
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
@WebMvcTest(AppWorkflowBindingApiController.class)
@WithMockUser
class AppWorkflowBindingApiControllerIntTest {

    private static final String WORKFLOW_UUID = "5b3f6a1e-0c2d-4e8f-9a7b-1d2c3e4f5a6b";

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
    void testGetAppBindableWorkflowsMapsEveryField() throws Exception {
        when(appWorkflowFacade.getBindableWorkflows(1L)).thenReturn(
            List.of(
                new AppBindableWorkflowDTO(
                    11L, "Refunds", "Refund project", WORKFLOW_UUID, "Approve refund", "Asks a manager")));

        mockMvc.perform(get("/v1/apps/1/bindable-workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].projectDeploymentId").value(11))
            .andExpect(jsonPath("$[0].projectDeploymentName").value("Refunds"))
            .andExpect(jsonPath("$[0].projectName").value("Refund project"))
            .andExpect(jsonPath("$[0].workflowUuid").value(WORKFLOW_UUID))
            .andExpect(jsonPath("$[0].workflowName").value("Approve refund"))
            .andExpect(jsonPath("$[0].description").value("Asks a manager"));
    }

    @Test
    void testGetAppBindableWorkflowsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(appWorkflowFacade.getBindableWorkflows(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/apps/1/bindable-workflows"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testGetAppWorkflowBindings() throws Exception {
        when(appWorkflowFacade.getWorkflowBindings(1L))
            .thenReturn(List.of(bindingDto(), unavailableBindingDto()));

        mockMvc.perform(get("/v1/apps/1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].appId").value(1))
            .andExpect(jsonPath("$[0].workflowKey").value("approve_refund"))
            .andExpect(jsonPath("$[0].projectDeploymentId").value(11))
            .andExpect(jsonPath("$[0].projectDeploymentName").value("Refunds"))
            .andExpect(jsonPath("$[0].workflowUuid").value(WORKFLOW_UUID))
            .andExpect(jsonPath("$[0].workflowName").value("Approve refund"))
            .andExpect(jsonPath("$[0].available").value(true))
            .andExpect(jsonPath("$[1].workflowKey").value("legacy"))
            .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test
    void testBindAppWorkflowPassesTheInput() throws Exception {
        when(appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, WORKFLOW_UUID)).thenReturn(bindingDto());

        mockMvc.perform(
            put("/v1/apps/1/workflows/approve_refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectDeploymentId\":11,\"workflowUuid\":\"" + WORKFLOW_UUID + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowKey").value("approve_refund"))
            .andExpect(jsonPath("$.projectDeploymentId").value(11))
            .andExpect(jsonPath("$.available").value(true));

        verify(appWorkflowFacade).bindWorkflow(1L, "approve_refund", 11L, WORKFLOW_UUID);
    }

    @Test
    void testBindAppWorkflowWithoutAUuidIsBadRequest() throws Exception {
        mockMvc.perform(
            put("/v1/apps/1/workflows/approve_refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectDeploymentId\":11}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(appWorkflowFacade);
    }

    @Test
    void testUnbindAppWorkflowReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/apps/1/workflows/approve_refund"))
            .andExpect(status().isNoContent());

        verify(appWorkflowFacade).unbindWorkflow(1L, "approve_refund");
    }

    @Test
    void testNotBindableIsBadRequest() throws Exception {
        when(appWorkflowFacade.bindWorkflow(1L, "approve_refund", 11L, WORKFLOW_UUID))
            .thenThrow(new ConfigurationException("not bindable", AppErrorType.APP_WORKFLOW_NOT_BINDABLE));

        mockMvc.perform(
            put("/v1/apps/1/workflows/approve_refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectDeploymentId\":11,\"workflowUuid\":\"" + WORKFLOW_UUID + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_NOT_BINDABLE"));
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

        when(appWorkflowFacade.bindWorkflow(anyLong(), anyString(), anyLong(), anyString()))
            .thenReturn(
                new AppWorkflowBindingDTO(7L, 1L, "approve_refund", 11L, WORKFLOW_UUID, "Refunds", "Approve refund",
                    true));

        mockMvc.perform(withEnvironment(route.request()
            .get(), requestEnvironment))
            .andExpect(status().is2xxSuccessful());

        InOrder inOrder = inOrder(appFacade, appWorkflowFacade);

        inOrder.verify(appFacade)
            .requireAppInEnvironment(1L, resolvedEnvironment);

        route.facadeCall()
            .accept(inOrder.verify(appWorkflowFacade));

        verifyNoMoreInteractions(appFacade);
    }

    private static Stream<Arguments> routesWithRequestEnvironment() {
        return routes().flatMap(route -> Stream.of(
            Arguments.of(route, "DEVELOPMENT", Environment.DEVELOPMENT),
            Arguments.of(route, null, Environment.PRODUCTION)));
    }

    private static Stream<Route> routes() {
        return Stream.of(
            new Route(
                "getAppBindableWorkflows", () -> get("/v1/apps/1/bindable-workflows"),
                facade -> facade.getBindableWorkflows(anyLong())),
            new Route(
                "getAppWorkflowBindings", () -> get("/v1/apps/1/workflows"),
                facade -> facade.getWorkflowBindings(anyLong())),
            new Route(
                "bindAppWorkflow",
                () -> put("/v1/apps/1/workflows/approve_refund").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"projectDeploymentId\":11,\"workflowUuid\":\"" + WORKFLOW_UUID + "\"}"),
                facade -> facade.bindWorkflow(anyLong(), anyString(), anyLong(), anyString())),
            new Route(
                "unbindAppWorkflow", () -> delete("/v1/apps/1/workflows/approve_refund"),
                facade -> facade.unbindWorkflow(anyLong(), anyString())));
    }

    private static MockHttpServletRequestBuilder withEnvironment(
        MockHttpServletRequestBuilder request, @Nullable String environment) {

        return environment == null ? request : request.header("X-Environment", environment);
    }

    private static AppWorkflowBindingDTO bindingDto() {
        return new AppWorkflowBindingDTO(
            7L, 1L, "approve_refund", 11L, WORKFLOW_UUID, "Refunds", "Approve refund", true);
    }

    private static AppWorkflowBindingDTO unavailableBindingDto() {
        return new AppWorkflowBindingDTO(8L, 1L, "legacy", 12L, "gone-uuid", null, null, false);
    }

    private record Route(
        String name, Supplier<MockHttpServletRequestBuilder> request, Consumer<AppWorkflowFacade> facadeCall) {

        @Override
        public String toString() {
            return name;
        }
    }
}
