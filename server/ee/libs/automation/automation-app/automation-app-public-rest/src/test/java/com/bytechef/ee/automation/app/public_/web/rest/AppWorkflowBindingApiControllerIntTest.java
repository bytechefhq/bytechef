/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
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

    private static final long APP_INSTANCE_ID = 30L;

    private static final String WORKFLOW_UUID = "5b3f6a1e-0c2d-4e8f-9a7b-1d2c3e4f5a6b";
    private static final String BIND_BODY =
        "{\"projectDeploymentId\":11,\"workflowUuid\":\"" + WORKFLOW_UUID + "\"}";

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
    void testGetAppInstanceBindableWorkflowsMapsEveryField() throws Exception {
        when(appWorkflowFacade.getBindableWorkflows(APP_INSTANCE_ID)).thenReturn(
            List.of(
                new AppBindableWorkflowDTO(
                    11L, "Refunds", "Refund project", WORKFLOW_UUID, "Approve refund", "Asks a manager")));

        mockMvc.perform(get("/v1/app-instances/30/bindable-workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].projectDeploymentId").value(11))
            .andExpect(jsonPath("$[0].projectDeploymentName").value("Refunds"))
            .andExpect(jsonPath("$[0].projectName").value("Refund project"))
            .andExpect(jsonPath("$[0].workflowUuid").value(WORKFLOW_UUID))
            .andExpect(jsonPath("$[0].workflowName").value("Approve refund"))
            .andExpect(jsonPath("$[0].description").value("Asks a manager"));
    }

    @Test
    void testGetAppInstanceBindableWorkflowsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(appWorkflowFacade.getBindableWorkflows(APP_INSTANCE_ID))
            .thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/app-instances/30/bindable-workflows"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testGetAppInstanceWorkflowBindings() throws Exception {
        when(appWorkflowFacade.getWorkflowBindings(APP_INSTANCE_ID))
            .thenReturn(List.of(bindingDto(), unavailableBindingDto()));

        mockMvc.perform(get("/v1/app-instances/30/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].appInstanceId").value(APP_INSTANCE_ID))
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
    void testBindAppInstanceWorkflowPassesTheInput() throws Exception {
        when(appWorkflowFacade.bindWorkflow(APP_INSTANCE_ID, "approve_refund", 11L, WORKFLOW_UUID))
            .thenReturn(bindingDto());

        mockMvc.perform(
            put("/v1/app-instances/30/workflows/approve_refund").header("X-Environment", "DEVELOPMENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BIND_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowKey").value("approve_refund"))
            .andExpect(jsonPath("$.projectDeploymentId").value(11))
            .andExpect(jsonPath("$.available").value(true));

        verify(appWorkflowFacade).bindWorkflow(APP_INSTANCE_ID, "approve_refund", 11L, WORKFLOW_UUID);
    }

    @Test
    void testBindAppInstanceWorkflowWithoutAUuidIsBadRequest() throws Exception {
        mockMvc.perform(
            put("/v1/app-instances/30/workflows/approve_refund").header("X-Environment", "DEVELOPMENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectDeploymentId\":11}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(appWorkflowFacade);
    }

    @Test
    void testUnbindAppInstanceWorkflowReturnsNoContent() throws Exception {
        mockMvc.perform(
            delete("/v1/app-instances/30/workflows/approve_refund").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNoContent());

        verify(appWorkflowFacade).unbindWorkflow(APP_INSTANCE_ID, "approve_refund");
    }

    @Test
    void testNotBindableIsBadRequest() throws Exception {
        when(appWorkflowFacade.bindWorkflow(APP_INSTANCE_ID, "approve_refund", 11L, WORKFLOW_UUID))
            .thenThrow(new ConfigurationException("not bindable", AppErrorType.APP_WORKFLOW_NOT_BINDABLE));

        mockMvc.perform(
            put("/v1/app-instances/30/workflows/approve_refund").header("X-Environment", "DEVELOPMENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BIND_BODY))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_NOT_BINDABLE"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void testInstanceOfAnotherEnvironmentIs404(Route route) throws Exception {
        doThrow(new ConfigurationException("App instance 30 not found", AppErrorType.APP_INSTANCE_NOT_FOUND))
            .when(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);

        mockMvc.perform(route.request("DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NOT_FOUND"));

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);
        verifyNoMoreInteractions(appInstanceFacade);
        verifyNoInteractions(appFacade, appWorkflowFacade);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void testByIdRouteChecksTheEnvironmentBeforeTheFacadeCall(Route route) throws Exception {
        when(appWorkflowFacade.bindWorkflow(anyLong(), anyString(), anyLong(), anyString())).thenReturn(bindingDto());

        mockMvc.perform(route.request("DEVELOPMENT"))
            .andExpect(status().is2xxSuccessful());

        InOrder inOrder = inOrder(appInstanceFacade, appWorkflowFacade);

        inOrder.verify(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT);

        Consumer<AppWorkflowFacade> facadeCall = route.facadeCall();

        facadeCall.accept(inOrder.verify(appWorkflowFacade));

        verifyNoInteractions(appFacade);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("bindingsOutsideDevelopment")
    void testBindOutsideDevelopmentIs403(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_DEVELOPMENT_ONLY"));
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("bindingsOutsideDevelopment")
    void testRefusalHappensBeforeTheFacadeIsCalled(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isForbidden());

        verifyNoInteractions(appFacade, appInstanceFacade, appWorkflowFacade);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("readsOutsideDevelopment")
    void testReadsOutsideDevelopmentAreAllowed(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().isOk());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(
            APP_INSTANCE_ID, Environment.valueOf(requestEnvironment));
    }

    private static Stream<Route> reads() {
        return Stream.of(
            new Route(
                "getAppInstanceBindableWorkflows",
                environment -> get("/v1/app-instances/30/bindable-workflows").header("X-Environment", environment),
                facade -> facade.getBindableWorkflows(APP_INSTANCE_ID)),
            new Route(
                "getAppInstanceWorkflowBindings",
                environment -> get("/v1/app-instances/30/workflows").header("X-Environment", environment),
                facade -> facade.getWorkflowBindings(APP_INSTANCE_ID)));
    }

    private static Stream<Route> bindings() {
        return Stream.of(
            new Route(
                "bindAppInstanceWorkflow",
                environment -> put("/v1/app-instances/30/workflows/approve_refund").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BIND_BODY),
                facade -> facade.bindWorkflow(eq(APP_INSTANCE_ID), anyString(), anyLong(), anyString())),
            new Route(
                "unbindAppInstanceWorkflow",
                environment -> delete("/v1/app-instances/30/workflows/approve_refund")
                    .header("X-Environment", environment),
                facade -> facade.unbindWorkflow(eq(APP_INSTANCE_ID), anyString())));
    }

    private static Stream<Route> routes() {
        return Stream.concat(reads(), bindings());
    }

    private static Stream<Arguments> bindingsOutsideDevelopment() {
        return withNonDevelopmentEnvironments(bindings());
    }

    private static Stream<Arguments> readsOutsideDevelopment() {
        return withNonDevelopmentEnvironments(reads());
    }

    private static Stream<Arguments> withNonDevelopmentEnvironments(Stream<Route> routes) {
        return routes.flatMap(
            route -> Stream.of("STAGING", "PRODUCTION")
                .map(environment -> Arguments.of(route, environment)));
    }

    private static AppWorkflowBindingDTO bindingDto() {
        return new AppWorkflowBindingDTO(
            7L, APP_INSTANCE_ID, "approve_refund", 11L, WORKFLOW_UUID, "Refunds", "Approve refund", true);
    }

    private static AppWorkflowBindingDTO unavailableBindingDto() {
        return new AppWorkflowBindingDTO(8L, APP_INSTANCE_ID, "legacy", 12L, "gone-uuid", null, null, false);
    }

    private record Route(
        String name, Function<String, MockHttpServletRequestBuilder> requestFactory,
        Consumer<AppWorkflowFacade> facadeCall) {

        MockHttpServletRequestBuilder request(String environment) {
            return requestFactory.apply(environment);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
