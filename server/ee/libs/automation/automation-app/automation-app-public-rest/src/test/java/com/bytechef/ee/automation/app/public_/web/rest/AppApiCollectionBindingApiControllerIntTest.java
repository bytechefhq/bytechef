/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppApiCollectionBindingApiController.class)
@WithMockUser
class AppApiCollectionBindingApiControllerIntTest {

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
    void testGetAppApiCollectionBindings() throws Exception {
        when(appApiCollectionFacade.getApiCollectionBindings(1L)).thenReturn(
            List.of(bindingDto(), new AppApiCollectionBindingDTO(8L, 1L, 22L, "#22", null, 0, 0L, null, false)));

        mockMvc.perform(get("/v1/apps/1/api-collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].appId").value(1))
            .andExpect(jsonPath("$[0].apiCollectionId").value(21))
            .andExpect(jsonPath("$[0].name").value("CRM"))
            .andExpect(jsonPath("$[0].contextPath").value("crm"))
            .andExpect(jsonPath("$[0].collectionVersion").value(2))
            .andExpect(jsonPath("$[0].projectId").value(3))
            .andExpect(jsonPath("$[0].projectName").value("Sales"))
            .andExpect(jsonPath("$[0].available").value(true))
            .andExpect(jsonPath("$[1].name").value("#22"))
            .andExpect(jsonPath("$[1].projectId").doesNotExist())
            .andExpect(jsonPath("$[1].available").value(false));
    }

    @Test
    void testGetAppBindableApiCollections() throws Exception {
        when(appApiCollectionFacade.getBindableApiCollections(1L))
            .thenReturn(List.of(new AppBindableApiCollectionDTO(21L, "CRM", "crm", 2, 3L, "Sales", false)));

        mockMvc.perform(get("/v1/apps/1/bindable-api-collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].apiCollectionId").value(21))
            .andExpect(jsonPath("$[0].name").value("CRM"))
            .andExpect(jsonPath("$[0].contextPath").value("crm"))
            .andExpect(jsonPath("$[0].collectionVersion").value(2))
            .andExpect(jsonPath("$[0].projectId").value(3))
            .andExpect(jsonPath("$[0].projectName").value("Sales"))
            .andExpect(jsonPath("$[0].available").value(false));
    }

    @Test
    void testBindAppApiCollectionReturnsTheBinding() throws Exception {
        when(appApiCollectionFacade.bindApiCollection(1L, 21L)).thenReturn(bindingDto());

        mockMvc.perform(put("/v1/apps/1/api-collections/21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiCollectionId").value(21))
            .andExpect(jsonPath("$.available").value(true));

        InOrder inOrder = inOrder(appFacade, appApiCollectionFacade);

        inOrder.verify(appFacade)
            .requireAppInEnvironment(1L, Environment.PRODUCTION);
        inOrder.verify(appApiCollectionFacade)
            .bindApiCollection(1L, 21L);
    }

    @Test
    void testBindAppApiCollectionNotBindableIsBadRequest() throws Exception {
        when(appApiCollectionFacade.bindApiCollection(1L, 21L)).thenThrow(
            new ConfigurationException("not bindable", AppErrorType.APP_API_COLLECTION_NOT_BINDABLE));

        mockMvc.perform(put("/v1/apps/1/api-collections/21"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_API_COLLECTION_NOT_BINDABLE"));
    }

    @Test
    void testUnbindAppApiCollectionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/v1/apps/1/api-collections/21"))
            .andExpect(status().isNoContent());

        verify(appApiCollectionFacade).unbindApiCollection(1L, 21L);
    }

    @Test
    void testAppOfAnotherEnvironmentIsNotFound() throws Exception {
        doThrow(new ConfigurationException("App 1 not found", AppErrorType.APP_NOT_FOUND)).when(appFacade)
            .requireAppInEnvironment(1L, Environment.DEVELOPMENT);

        mockMvc.perform(get("/v1/apps/1/api-collections").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_NOT_FOUND"));

        verifyNoInteractions(appApiCollectionFacade);
    }

    private static AppApiCollectionBindingDTO bindingDto() {
        return new AppApiCollectionBindingDTO(7L, 1L, 21L, "CRM", "crm", 2, 3L, "Sales", true);
    }
}
