/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.config.AutomationDataTablePublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationDataTablePublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(DataTableApiController.class)
@MockitoBean(types = DataTableService.class)
class DataTableApiControllerIntTest {

    private static final long WORKSPACE_ID = 100L;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @ParameterizedTest(name = "X-Environment [{0}]")
    @ValueSource(strings = {
        "development", "Development", " development "
    })
    void testListDataTablesAcceptsAnyCaseOfTheEnvironmentHeader(String requestEnvironment) throws Exception {
        when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);

        mockMvc
            .perform(
                get("/v1/workspaces/{workspaceId}/data-tables", WORKSPACE_ID)
                    .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).listTables(WORKSPACE_ID, Environment.DEVELOPMENT.ordinal());
    }

    @ParameterizedTest(name = "X-Environment [{0}]")
    @ValueSource(strings = {
        "", "   "
    })
    void testListDataTablesTreatsABlankEnvironmentHeaderAsAbsent(String requestEnvironment) throws Exception {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);

        mockMvc
            .perform(
                get("/v1/workspaces/{workspaceId}/data-tables", WORKSPACE_ID)
                    .header("X-Environment", requestEnvironment))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).listTables(WORKSPACE_ID, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testListDataTablesRejectsAnUnknownEnvironmentHeader() throws Exception {
        mockMvc
            .perform(
                get("/v1/workspaces/{workspaceId}/data-tables", WORKSPACE_ID)
                    .header("X-Environment", "staging-x"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(environmentService, workspaceDataTableFacade);
    }
}
