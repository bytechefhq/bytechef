/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.configuration.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.configuration.dto.ClusterElementOutputDTO;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeOutputModel;
import com.bytechef.platform.domain.BaseProperty;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.ConversionService;

/**
 * @author Ivica Cardic
 */
class WorkflowNodeOutputApiControllerTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String WORKFLOW_ID = "workflow1";

    private final ConversionService conversionService = mock(ConversionService.class);
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);

    private WorkflowNodeOutputApiController workflowNodeOutputApiController;

    @BeforeEach
    void setUp() {
        workflowNodeOutputApiController = new WorkflowNodeOutputApiController(
            conversionService, workflowNodeOutputFacade);

        when(conversionService.convert(any(WorkflowNodeOutputDTO.class), eq(WorkflowNodeOutputModel.class)))
            .thenReturn(new WorkflowNodeOutputModel());
    }

    @Test
    void testGetClusterElementOutputPassesTestOutputResponseThrough() {
        assertTrue(getClusterElementOutputDTO(true).testOutputResponse());
    }

    @Test
    void testGetClusterElementOutputPassesMissingTestOutputResponseThrough() {
        assertFalse(getClusterElementOutputDTO(false).testOutputResponse());
    }

    private WorkflowNodeOutputDTO getClusterElementOutputDTO(boolean testOutputResponse) {
        ClusterElementOutputDTO clusterElementOutputDTO = new ClusterElementOutputDTO(
            mock(ClusterElementDefinition.class), mock(BaseProperty.class), null, Map.of("id", "contact-42"),
            testOutputResponse, "hubspot_1");

        when(workflowNodeOutputFacade.getClusterElementOutput(
            WORKFLOW_ID, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID)).thenReturn(clusterElementOutputDTO);

        workflowNodeOutputApiController.getClusterElementOutput(
            WORKFLOW_ID, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID);

        ArgumentCaptor<WorkflowNodeOutputDTO> workflowNodeOutputDTOArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowNodeOutputDTO.class);

        verify(conversionService).convert(
            workflowNodeOutputDTOArgumentCaptor.capture(), eq(WorkflowNodeOutputModel.class));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputDTOArgumentCaptor.getValue();

        assertEquals("hubspot_1", workflowNodeOutputDTO.workflowNodeName());

        return workflowNodeOutputDTO;
    }
}
