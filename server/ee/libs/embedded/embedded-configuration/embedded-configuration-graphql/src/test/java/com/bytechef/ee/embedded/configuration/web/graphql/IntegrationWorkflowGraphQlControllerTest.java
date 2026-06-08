/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationWorkflowGraphQlControllerTest {

    @Test
    void testUpdateIntegrationWorkflowPermissionExpressionReturnsDTO() {
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        IntegrationWorkflowDTO integrationWorkflowDTO = mock(IntegrationWorkflowDTO.class);

        when(integrationWorkflowFacade.getIntegrationWorkflow(42L)).thenReturn(integrationWorkflowDTO);

        IntegrationWorkflowGraphQlController controller = new IntegrationWorkflowGraphQlController(
            integrationWorkflowFacade, integrationWorkflowService);

        IntegrationWorkflowDTO result = controller.updateIntegrationWorkflowPermissionExpression(
            42L, "metadata['tier'] == 'pro'");

        // The mutation must return the DTO (not the domain IntegrationWorkflow) so the IntegrationWorkflow type's
        // @SchemaMapping field resolvers, which declare IntegrationWorkflowDTO as their source, can resolve the
        // selected sub-fields. Returning the domain object triggers a Spring GraphQL source-type mismatch.
        assertThat(result).isSameAs(integrationWorkflowDTO);

        verify(integrationWorkflowService).updatePermissionExpression(42L, "metadata['tier'] == 'pro'");
        verify(integrationWorkflowFacade).getIntegrationWorkflow(42L);
    }
}
