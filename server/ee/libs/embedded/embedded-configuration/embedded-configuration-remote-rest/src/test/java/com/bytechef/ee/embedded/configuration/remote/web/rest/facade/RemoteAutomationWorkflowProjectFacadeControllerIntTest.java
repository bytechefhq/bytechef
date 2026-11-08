/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = RemoteAutomationWorkflowProjectFacadeControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteAutomationWorkflowProjectFacadeController.class)
class RemoteAutomationWorkflowProjectFacadeControllerIntTest {

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetPublishedProjects() throws Exception {
        AutomationWorkflowProjectDTO automationWorkflowProjectDTO = new AutomationWorkflowProjectDTO(
            7L, "Sales", "Sales automations", null, List.of(), true, 2, 1, List.of(), null, true, 0, null);

        when(automationWorkflowProjectFacade.getPublishedProjects()).thenReturn(List.of(automationWorkflowProjectDTO));

        mockMvc
            .perform(
                get("/remote/automation-workflow-project-facade/get-published-projects")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].name").value("Sales"))
            .andExpect(jsonPath("$[0].published").value(true));
    }

    @Configuration
    @Import(RemoteAutomationWorkflowProjectFacadeController.class)
    static class RemoteControllerTestConfiguration {
    }
}
