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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * getPreviousWorkflowNodeOutputs is the interesting case: the facade method it reads through is {@code @Cacheable},
 * keyed from the raw method arguments, so environmentId is deliberately NOT resolved inside the facade (see
 * WorkflowNodeOutputFacadeImpl) -- it must be resolved here, once, before both the cache-eviction call
 * (checkWorkflowCache) and the cached read, so the two can never disagree about which environment's cache entry they
 * touch. This test asserts BOTH calls receive the identical effective value.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeOutputApiControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long ENVIRONMENT_ID = 1L;
    private static final long PRODUCTION_ORDINAL = 2L;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetClusterElementOutputPassesTestOutputResponseThrough() {
        assertTrue(getClusterElementOutputDTO(true).testOutputResponse());
    }

    @Test
    void testGetClusterElementOutputPassesMissingTestOutputResponseThrough() {
        assertFalse(getClusterElementOutputDTO(false).testOutputResponse());
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsUsesConfinedPrincipalEnvironmentForBothEvictionAndRead() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(List.of());

        workflowNodeOutputApiController.getPreviousWorkflowNodeOutputs("workflow-1", DEVELOPMENT_ORDINAL, "node-1");

        ArgumentCaptor<Long> evictionEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> readEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeOutputFacade).checkWorkflowCache(
            eq("workflow-1"), eq("node-1"), evictionEnvironmentIdCaptor.capture());
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            eq("workflow-1"), eq("node-1"), readEnvironmentIdCaptor.capture());

        assertThat(evictionEnvironmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
        assertThat(readEnvironmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsHonoursSessionPrincipalRequestedEnvironmentForBoth() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(List.of());

        workflowNodeOutputApiController.getPreviousWorkflowNodeOutputs("workflow-1", DEVELOPMENT_ORDINAL, "node-1");

        ArgumentCaptor<Long> evictionEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> readEnvironmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeOutputFacade).checkWorkflowCache(
            eq("workflow-1"), eq("node-1"), evictionEnvironmentIdCaptor.capture());
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            eq("workflow-1"), eq("node-1"), readEnvironmentIdCaptor.capture());

        assertThat(evictionEnvironmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
        assertThat(readEnvironmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
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

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
