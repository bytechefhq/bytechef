/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.domain.AppDeploymentProviderType;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppDeploymentFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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
@ContextConfiguration(
    classes = {
        JacksonConfiguration.class, AppDeploymentApiController.class, EnvironmentModelConverter.class
    })
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppDeploymentApiController.class)
@WithMockUser
class AppDeploymentApiControllerIntTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FINISHED_DATE = Instant.parse("2026-01-01T00:05:00Z");

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 30L;
    private static final long APP_VERSION_ID = 40L;
    private static final long APP_DEPLOYMENT_ID = 50L;

    private static final String VERSION_BODY =
        "{\"name\":\"1.1\",\"gitRef\":\"main\",\"gitCommitSha\":\"abc123\",\"description\":\"First release\"}";

    private static final String TRIGGER_BODY = "{\"appVersionId\":40}";

    private static final String DEPLOYMENT_BODY =
        "{\"versionName\":\"1.1\",\"url\":\"https://crm.example.com\",\"gitCommitSha\":\"abc123\"}";

    @MockitoBean
    private AppDeploymentFacade appDeploymentFacade;

    @MockitoBean
    private AppInstanceFacade appInstanceFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        when(appDeploymentFacade.getVersions(APP_ID, 50)).thenReturn(List.of(appVersionDto()));
        when(appDeploymentFacade.createVersion(anyLong(), anyString(), any(), any(), any()))
            .thenReturn(appVersionDto());
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 50)).thenReturn(List.of(appDeploymentDto()));
        when(appDeploymentFacade.recordDeployment(anyLong(), anyString(), any(), any()))
            .thenReturn(appDeploymentDto());
        when(appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID)).thenReturn(queuedDeploymentDto());
        when(appDeploymentFacade.rollback(APP_INSTANCE_ID, APP_VERSION_ID)).thenReturn(queuedDeploymentDto());
        when(appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)).thenReturn(appDeploymentDto());
        when(appDeploymentFacade.getDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)).thenReturn(appDeploymentDto());
        when(appDeploymentFacade.fetchVersion(APP_ID, "1.1")).thenReturn(Optional.of(appVersionDto()));
        when(appDeploymentFacade.getVersionsForInstance(APP_INSTANCE_ID, 50)).thenReturn(List.of(appVersionDto()));
        when(appDeploymentFacade.fetchVersionForInstance(APP_INSTANCE_ID, "1.1"))
            .thenReturn(Optional.of(appVersionDto()));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testDeployIsAllowedInProduction(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deploy").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(APP_DEPLOYMENT_ID))
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.provider").value("VERCEL"))
            .andExpect(jsonPath("$.appVersion.name").value("1.1"));

        verify(appInstanceFacade).requireAppInstanceInEnvironment(
            APP_INSTANCE_ID, Environment.valueOf(requestEnvironment));
        verify(appDeploymentFacade).deploy(APP_INSTANCE_ID, APP_VERSION_ID);
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testRollbackAndCancelAreAllowedInProduction(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/rollback").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("QUEUED"));

        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments/50/cancel").header("X-Environment", requestEnvironment))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(APP_DEPLOYMENT_ID));

        verify(appDeploymentFacade).rollback(APP_INSTANCE_ID, APP_VERSION_ID);
        verify(appDeploymentFacade).cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID);
    }

    @Test
    void testGetAppInstanceDeploymentReturnsTheDeployment() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30/deployments/50").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(APP_DEPLOYMENT_ID))
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.metadata").doesNotExist());
    }

    @Test
    void testDeployWithoutAVersionIdIs400() throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deploy").header("X-Environment", "PRODUCTION")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appDeploymentFacade);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("triggerConflicts")
    void testDeployAndRollbackConflictsAre409(String errorName, AppErrorType appErrorType) throws Exception {
        when(appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .thenThrow(new ConfigurationException("Refused", appErrorType));
        when(appDeploymentFacade.rollback(APP_INSTANCE_ID, APP_VERSION_ID))
            .thenThrow(new ConfigurationException("Refused", appErrorType));

        for (String path : List.of("/v1/app-instances/30/deploy", "/v1/app-instances/30/rollback")) {
            mockMvc
                .perform(
                    post(path).header("X-Environment", "PRODUCTION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIGGER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(errorName));
        }
    }

    @Test
    void testDeployOfAVersionOfAnotherAppIs404() throws Exception {
        when(appDeploymentFacade.deploy(APP_INSTANCE_ID, APP_VERSION_ID))
            .thenThrow(new ConfigurationException("App version 40 not found", AppErrorType.APP_VERSION_NOT_FOUND));

        mockMvc
            .perform(
                post("/v1/app-instances/30/deploy").header("X-Environment", "PRODUCTION")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_VERSION_NOT_FOUND"));
    }

    @Test
    void testRollbackToAVersionNeverReadyOnTheInstanceIs404() throws Exception {
        when(appDeploymentFacade.rollback(APP_INSTANCE_ID, APP_VERSION_ID))
            .thenThrow(new ConfigurationException("Never deployed", AppErrorType.APP_VERSION_NOT_FOUND));

        mockMvc
            .perform(
                post("/v1/app-instances/30/rollback").header("X-Environment", "PRODUCTION")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_VERSION_NOT_FOUND"));
    }

    @Test
    void testCancelOrGetOfADeploymentOfAnotherInstanceIs404() throws Exception {
        ConfigurationException notFound = new ConfigurationException(
            "App deployment 50 not found", AppErrorType.APP_DEPLOYMENT_NOT_FOUND);

        when(appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)).thenThrow(notFound);
        when(appDeploymentFacade.getDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)).thenThrow(notFound);

        mockMvc.perform(post("/v1/app-instances/30/deployments/50/cancel").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_NOT_FOUND"));
        mockMvc.perform(get("/v1/app-instances/30/deployments/50").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_NOT_FOUND"));
    }

    @Test
    void testCancelOfATerminalDeploymentIs409AndAProviderRefusalIs502() throws Exception {
        when(appDeploymentFacade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID))
            .thenThrow(new ConfigurationException("Terminal", AppErrorType.APP_DEPLOYMENT_TERMINAL))
            .thenThrow(new ConfigurationException("Refused", AppErrorType.APP_DEPLOYMENT_PROVIDER_FAILED));

        mockMvc.perform(post("/v1/app-instances/30/deployments/50/cancel").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_TERMINAL"));
        mockMvc.perform(post("/v1/app-instances/30/deployments/50/cancel").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_PROVIDER_FAILED"));
    }

    @Test
    void testGetAppVersionsWithANameAsksTheFacadeForThatVersionOnly() throws Exception {
        mockMvc.perform(get("/v1/apps/1/versions").param("name", "1.1")
            .header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(APP_VERSION_ID));

        when(appDeploymentFacade.fetchVersion(APP_ID, "9.9")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/apps/1/versions").param("name", "9.9")
            .header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(appDeploymentFacade, never()).getVersions(anyLong(), anyInt());
    }

    @Test
    void testGetAppVersionsListsTheVersionsOfTheApp() throws Exception {
        mockMvc.perform(get("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(APP_VERSION_ID))
            .andExpect(jsonPath("$[0].appId").value(APP_ID))
            .andExpect(jsonPath("$[0].name").value("1.1"))
            .andExpect(jsonPath("$[0].gitRef").value("main"))
            .andExpect(jsonPath("$[0].gitCommitSha").value("abc123"))
            .andExpect(jsonPath("$[0].description").value("First release"))
            .andExpect(jsonPath("$[0].createdBy").value("alice"))
            .andExpect(jsonPath("$[0].createdDate").exists());

        verify(appDeploymentFacade).getVersions(APP_ID, 50);
        verifyNoInteractions(appInstanceFacade);
    }

    @Test
    void testGetAppInstanceVersionsListsTheVersionsOfTheInstancesApp() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30/versions").header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(APP_VERSION_ID))
            .andExpect(jsonPath("$[0].appId").value(APP_ID))
            .andExpect(jsonPath("$[0].name").value("1.1"));

        verify(appDeploymentFacade).getVersionsForInstance(APP_INSTANCE_ID, 50);
        verify(appDeploymentFacade, never()).getVersions(anyLong(), anyInt());
    }

    @Test
    void testGetAppInstanceVersionsWithANameAsksTheFacadeForThatVersionOnly() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30/versions").param("name", "1.1")
            .header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(APP_VERSION_ID));

        when(appDeploymentFacade.fetchVersionForInstance(APP_INSTANCE_ID, "9.9")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/app-instances/30/versions").param("name", "9.9")
            .header("X-Environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(appDeploymentFacade, never()).getVersionsForInstance(anyLong(), anyInt());
        verify(appDeploymentFacade, never()).fetchVersion(anyLong(), anyString());
    }

    @Test
    void testCreateAppVersionPassesTheBodyAndReturnsCreated() throws Exception {
        mockMvc
            .perform(
                post("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VERSION_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(APP_VERSION_ID))
            .andExpect(jsonPath("$.name").value("1.1"));

        verify(appDeploymentFacade).createVersion(APP_ID, "1.1", "main", "abc123", "First release");
        verifyNoMoreInteractions(appDeploymentFacade);
        verifyNoInteractions(appInstanceFacade);
    }

    @Test
    void testCreateAppVersionWithoutANameIs400() throws Exception {
        mockMvc
            .perform(
                post("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gitCommitSha\":\"abc123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(appDeploymentFacade, never()).createVersion(anyLong(), any(), any(), any(), any());
    }

    @Test
    void testCreateAppVersionOfANameTakenByAnotherCommitIs409() throws Exception {
        when(appDeploymentFacade.createVersion(anyLong(), anyString(), any(), any(), any())).thenThrow(
            new ConfigurationException(
                "App version '1.1' already exists", AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS));

        mockMvc
            .perform(
                post("/v1/apps/1/versions").header("X-Environment", "PRODUCTION")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VERSION_BODY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APP_VERSION_NAME_ALREADY_EXISTS"));
    }

    @Test
    void testGetAppInstanceDeploymentsCarriesTheVersionNameAndNoMetadata() throws Exception {
        mockMvc.perform(get("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(APP_DEPLOYMENT_ID))
            .andExpect(jsonPath("$[0].appInstanceId").value(APP_INSTANCE_ID))
            .andExpect(jsonPath("$[0].appVersion.id").value(APP_VERSION_ID))
            .andExpect(jsonPath("$[0].appVersion.name").value("1.1"))
            .andExpect(jsonPath("$[0].provider").value("EXTERNAL"))
            .andExpect(jsonPath("$[0].status").value("READY"))
            .andExpect(jsonPath("$[0].url").value("https://crm.example.com"))
            .andExpect(jsonPath("$[0].current").value(true))
            .andExpect(jsonPath("$[0].createdBy").value("alice"))
            .andExpect(jsonPath("$[0].createdDate").exists())
            .andExpect(jsonPath("$[0].finishedDate").exists())
            .andExpect(jsonPath("$[0].metadata").doesNotExist());
    }

    @Test
    void testRecordAppInstanceDeploymentPassesTheBodyAndReturnsCreated() throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(DEPLOYMENT_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(APP_DEPLOYMENT_ID))
            .andExpect(jsonPath("$.appVersion.name").value("1.1"))
            .andExpect(jsonPath("$.current").value(true));

        verify(appDeploymentFacade).recordDeployment(APP_INSTANCE_ID, "1.1", "https://crm.example.com", "abc123");
    }

    @Test
    void testRecordAppInstanceDeploymentWithoutAVersionNameIs400() throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"url\":\"https://crm.example.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(appDeploymentFacade, appInstanceFacade);
    }

    @Test
    void testRecordAppInstanceDeploymentWithAUrlThatIsNotHttpIs400() throws Exception {
        when(appDeploymentFacade.recordDeployment(anyLong(), anyString(), any(), any())).thenThrow(
            new ConfigurationException(
                "The deployment url must be an http or https URL", AppErrorType.APP_DEPLOYMENT_URL_INVALID));

        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").header("X-Environment", "PRODUCTION")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"versionName\":\"1.1\",\"url\":\"javascript:alert(1)\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_DEPLOYMENT_URL_INVALID"));
    }

    @ParameterizedTest(name = "X-Environment {0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testRecordDeploymentAndCreateVersionAreAllowedInProduction(String requestEnvironment) throws Exception {
        mockMvc
            .perform(
                post("/v1/apps/1/versions").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VERSION_BODY))
            .andExpect(status().isCreated());

        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").header("X-Environment", requestEnvironment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(DEPLOYMENT_BODY))
            .andExpect(status().isCreated());

        verify(appDeploymentFacade).createVersion(APP_ID, "1.1", "main", "abc123", "First release");
        verify(appInstanceFacade).requireAppInstanceInEnvironment(
            APP_INSTANCE_ID, Environment.valueOf(requestEnvironment));
        verify(appDeploymentFacade).recordDeployment(APP_INSTANCE_ID, "1.1", "https://crm.example.com", "abc123");
    }

    @Test
    void testRecordWithoutAnEnvironmentHeaderIsScopedToProduction() throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").contentType(MediaType.APPLICATION_JSON)
                    .content(DEPLOYMENT_BODY))
            .andExpect(status().isCreated());

        verify(appInstanceFacade).requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION);
    }

    @ParameterizedTest(name = "{0} with X-Environment {1}")
    @MethodSource("routesInEveryEnvironment")
    void testEveryRouteIsAllowedInEveryEnvironment(Route route, String requestEnvironment) throws Exception {
        mockMvc.perform(route.request(requestEnvironment))
            .andExpect(status().is2xxSuccessful());

        route.facadeCall()
            .accept(verify(appDeploymentFacade));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("deploymentRoutes")
    void testDeploymentRouteOfAnInstanceOfAnotherEnvironmentIs404(Route route) throws Exception {
        doThrow(new ConfigurationException("App instance 30 not found", AppErrorType.APP_INSTANCE_NOT_FOUND))
            .when(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.STAGING);

        mockMvc.perform(route.request("STAGING"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NOT_FOUND"));

        verifyNoInteractions(appDeploymentFacade);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("deploymentRoutes")
    void testDeploymentRouteOfAnotherEnvironmentIs404NotForbiddenWhenTheGuardedFacadeWouldDeny(Route route)
        throws Exception {

        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, 50)).thenThrow(new AccessDeniedException("Denied"));
        when(appDeploymentFacade.recordDeployment(anyLong(), anyString(), any(), any()))
            .thenThrow(new AccessDeniedException("Denied"));
        doThrow(new ConfigurationException("App instance 30 not found", AppErrorType.APP_INSTANCE_NOT_FOUND))
            .when(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION);

        mockMvc.perform(route.request("PRODUCTION"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_INSTANCE_NOT_FOUND"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("deploymentRoutes")
    void testDeploymentRouteChecksTheEnvironmentBeforeTheGuardedFacadeCall(Route route) throws Exception {
        mockMvc.perform(route.request("PRODUCTION"))
            .andExpect(status().is2xxSuccessful());

        InOrder inOrder = inOrder(appInstanceFacade, appDeploymentFacade);

        inOrder.verify(appInstanceFacade)
            .requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION);

        route.facadeCall()
            .accept(inOrder.verify(appDeploymentFacade));

        verifyNoMoreInteractions(appInstanceFacade, appDeploymentFacade);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versionRoutes")
    void testVersionRouteIsNotScopedToAnInstanceEnvironment(Route route) throws Exception {
        mockMvc.perform(route.request("STAGING"))
            .andExpect(status().is2xxSuccessful());

        route.facadeCall()
            .accept(verify(appDeploymentFacade));

        verifyNoMoreInteractions(appDeploymentFacade);
        verifyNoInteractions(appInstanceFacade);
    }

    @Test
    void testListsWithoutALimitAskTheFacadeForTheDefaultFifty() throws Exception {
        mockMvc.perform(get("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk());

        verify(appDeploymentFacade).getVersions(APP_ID, AppDeploymentFacade.DEFAULT_LIST_LIMIT);
        verify(appDeploymentFacade).getDeployments(APP_INSTANCE_ID, AppDeploymentFacade.DEFAULT_LIST_LIMIT);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 7, 200
    })
    void testListsPassTheGivenLimitToTheFacade(int limit) throws Exception {
        when(appDeploymentFacade.getVersions(APP_ID, limit)).thenReturn(List.of(appVersionDto()));
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, limit)).thenReturn(List.of(appDeploymentDto()));

        mockMvc
            .perform(
                get("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT")
                    .param("limit", String.valueOf(limit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(APP_VERSION_ID));
        mockMvc
            .perform(
                get("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT")
                    .param("limit", String.valueOf(limit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(APP_DEPLOYMENT_ID));

        verify(appDeploymentFacade).getVersions(APP_ID, limit);
        verify(appDeploymentFacade).getDeployments(APP_INSTANCE_ID, limit);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        -1, 0, 201
    })
    void testListsWithALimitTheFacadeRefusesAre400(int limit) throws Exception {
        ConfigurationException invalidLimit = new ConfigurationException(
            "The limit must be from 1 to 200", AppErrorType.APP_LIST_LIMIT_INVALID);

        when(appDeploymentFacade.getVersions(APP_ID, limit)).thenThrow(invalidLimit);
        when(appDeploymentFacade.getDeployments(APP_INSTANCE_ID, limit)).thenThrow(invalidLimit);

        mockMvc
            .perform(
                get("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT")
                    .param("limit", String.valueOf(limit)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_LIST_LIMIT_INVALID"));
        mockMvc
            .perform(
                get("/v1/app-instances/30/deployments").header("X-Environment", "DEVELOPMENT")
                    .param("limit", String.valueOf(limit)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_LIST_LIMIT_INVALID"));
    }

    @Test
    void testListWithALimitThatIsNotANumberIs400() throws Exception {
        mockMvc
            .perform(
                get("/v1/apps/1/versions").header("X-Environment", "DEVELOPMENT")
                    .param("limit", "many"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appDeploymentFacade);
    }

    @Test
    void testUnknownEnvironmentHeaderIs400() throws Exception {
        mockMvc
            .perform(
                post("/v1/app-instances/30/deployments").header("X-Environment", "MOON")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(DEPLOYMENT_BODY))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appDeploymentFacade, appInstanceFacade);
    }

    private static Stream<Route> versionRoutes() {
        return Stream.of(
            new Route(
                "getAppVersions", environment -> get("/v1/apps/1/versions").header("X-Environment", environment),
                facade -> facade.getVersions(APP_ID, 50)),
            new Route(
                "createAppVersion",
                environment -> post("/v1/apps/1/versions").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VERSION_BODY),
                facade -> facade.createVersion(APP_ID, "1.1", "main", "abc123", "First release")));
    }

    private static Stream<Route> deploymentRoutes() {
        return Stream.of(
            new Route(
                "getAppInstanceVersions",
                environment -> get("/v1/app-instances/30/versions").header("X-Environment", environment),
                facade -> facade.getVersionsForInstance(APP_INSTANCE_ID, 50)),
            new Route(
                "getAppInstanceDeployments",
                environment -> get("/v1/app-instances/30/deployments").header("X-Environment", environment),
                facade -> facade.getDeployments(APP_INSTANCE_ID, 50)),
            new Route(
                "recordAppInstanceDeployment",
                environment -> post("/v1/app-instances/30/deployments").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(DEPLOYMENT_BODY),
                facade -> facade.recordDeployment(APP_INSTANCE_ID, "1.1", "https://crm.example.com", "abc123")),
            new Route(
                "getAppInstanceDeployment",
                environment -> get("/v1/app-instances/30/deployments/50").header("X-Environment", environment),
                facade -> facade.getDeployment(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)),
            new Route(
                "deployAppInstance",
                environment -> post("/v1/app-instances/30/deploy").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY),
                facade -> facade.deploy(APP_INSTANCE_ID, APP_VERSION_ID)),
            new Route(
                "rollbackAppInstance",
                environment -> post("/v1/app-instances/30/rollback").header("X-Environment", environment)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TRIGGER_BODY),
                facade -> facade.rollback(APP_INSTANCE_ID, APP_VERSION_ID)),
            new Route(
                "cancelAppInstanceDeployment",
                environment -> post("/v1/app-instances/30/deployments/50/cancel").header("X-Environment", environment),
                facade -> facade.cancel(APP_INSTANCE_ID, APP_DEPLOYMENT_ID)));
    }

    private static Stream<Arguments> triggerConflicts() {
        return Stream.of(
            Arguments.of("APP_DEPLOYMENT_IN_PROGRESS", AppErrorType.APP_DEPLOYMENT_IN_PROGRESS),
            Arguments.of("APP_DEPLOYMENT_PROVIDER_UNAVAILABLE", AppErrorType.APP_DEPLOYMENT_PROVIDER_UNAVAILABLE),
            Arguments.of("APP_INSTANCE_TERMINATED", AppErrorType.APP_INSTANCE_TERMINATED));
    }

    private static Stream<Arguments> routesInEveryEnvironment() {
        return Stream.concat(versionRoutes(), deploymentRoutes())
            .flatMap(route -> Stream.of("DEVELOPMENT", "STAGING", "PRODUCTION")
                .map(environment -> Arguments.of(route, environment)));
    }

    private static AppVersionDTO appVersionDto() {
        return new AppVersionDTO(
            APP_VERSION_ID, APP_ID, "1.1", "main", "abc123", "First release", "alice", CREATED_DATE);
    }

    private static AppDeploymentDTO appDeploymentDto() {
        return new AppDeploymentDTO(
            APP_DEPLOYMENT_ID, APP_INSTANCE_ID, appVersionDto(), AppDeploymentProviderType.EXTERNAL, null,
            "https://crm.example.com", AppDeploymentStatus.READY, null, Map.of("internal", "value"), true, "alice",
            CREATED_DATE, FINISHED_DATE);
    }

    private static AppDeploymentDTO queuedDeploymentDto() {
        return new AppDeploymentDTO(
            APP_DEPLOYMENT_ID, APP_INSTANCE_ID, appVersionDto(), AppDeploymentProviderType.VERCEL, null, null,
            AppDeploymentStatus.QUEUED, null, Map.of(), false, "alice", CREATED_DATE, null);
    }

    private record Route(
        String name, Function<String, MockHttpServletRequestBuilder> requestFactory,
        Consumer<AppDeploymentFacade> facadeCall) {

        MockHttpServletRequestBuilder request(String environment) {
            return requestFactory.apply(environment);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
