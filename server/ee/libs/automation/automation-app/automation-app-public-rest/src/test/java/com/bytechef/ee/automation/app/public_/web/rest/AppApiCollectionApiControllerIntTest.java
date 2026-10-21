/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppApiCollectionDTO;
import com.bytechef.ee.automation.app.dto.AppApiEndpointDTO;
import com.bytechef.ee.automation.app.facade.AppApiExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppExecutionRestTestConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
@ContextConfiguration(classes = AppExecutionRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppApiCollectionApiController.class)
@WithMockUser
class AppApiCollectionApiControllerIntTest {

    @MockitoBean
    private AppApiExecutionFacade appApiExecutionFacade;

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testListsBoundCollectionsWithEndpointsAndSchemas() throws Exception {
        when(appApiExecutionFacade.getApiCollections()).thenReturn(
            List.of(
                new AppApiCollectionDTO(
                    21L, "CRM", "crm", 1, true,
                    List.of(
                        new AppApiEndpointDTO(
                            "getContact", "GET", "contacts/{id}",
                            Map.of("parameters", Map.of("type", "object")),
                            Map.of("success", Map.of("type", "object"))),
                        new AppApiEndpointDTO("createContact", "POST", "contacts", null, null))),
                new AppApiCollectionDTO(99L, "#99", null, 0, false, List.of())));

        mockMvc.perform(get("/api/app/v1/api-collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].apiCollectionId").value(21))
            .andExpect(jsonPath("$[0].name").value("CRM"))
            .andExpect(jsonPath("$[0].contextPath").value("crm"))
            .andExpect(jsonPath("$[0].collectionVersion").value(1))
            .andExpect(jsonPath("$[0].available").value(true))
            .andExpect(jsonPath("$[0].endpoints", hasSize(2)))
            .andExpect(jsonPath("$[0].endpoints[0].name").value("getContact"))
            .andExpect(jsonPath("$[0].endpoints[0].httpMethod").value("GET"))
            .andExpect(jsonPath("$[0].endpoints[0].path").value("contacts/{id}"))
            .andExpect(jsonPath("$[0].endpoints[0].requestSchema.parameters.type").value("object"))
            .andExpect(jsonPath("$[0].endpoints[0].responseSchema.success.type").value("object"))
            .andExpect(jsonPath("$[0].endpoints[1].requestSchema").isEmpty())
            .andExpect(jsonPath("$[0].endpoints[1].responseSchema").isEmpty())
            .andExpect(jsonPath("$[1].name").value("#99"))
            .andExpect(jsonPath("$[1].contextPath").isEmpty())
            .andExpect(jsonPath("$[1].available").value(false))
            .andExpect(jsonPath("$[1].endpoints", hasSize(0)));
    }
}
