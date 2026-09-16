/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * A denial on an item URL must answer 404, not 403: table names are guessable, so a 403 there is an existence oracle
 * over every workspace the caller cannot see.
 *
 * <p>
 * The sibling unit test calls {@code handleAccessDenied} directly, which proves only that the method body branches on
 * the URI. This one goes through MockMvc, so Spring's {@code ExceptionHandlerExceptionResolver} does the selecting --
 * the half that a direct call cannot reach, and the half that would break if the handler were moved, renamed or shaded
 * by an advice.
 *
 * <p>
 * The 403 half is deliberately not tested here: a rethrow from the handler escapes to Spring Security's
 * {@code ExceptionTranslationFilter}, which is not part of a standalone MockMvc chain. It is also the safe failure mode
 * -- the 404 is the leak-relevant one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataTableApiControllerAccessDeniedTest {

    private final WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
    private final DataTableService dataTableService = mock(DataTableService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenThrow(new AccessDeniedException("denied"));

        DataTableApiController dataTableApiController = new DataTableApiController(
            facade, new DataTableApiSupport(dataTableService, environmentService));

        mockMvc = MockMvcBuilders.standaloneSetup(dataTableApiController)
            .addPlaceholderValue("openapi.openAPIDefinition.base-path.automation", "/api/automation")
            .build();
    }

    @Test
    void testAccessDeniedOnAnItemUrlIs404() throws Exception {
        mockMvc.perform(get("/api/automation/v1/data-tables/orders"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()));
    }
}
