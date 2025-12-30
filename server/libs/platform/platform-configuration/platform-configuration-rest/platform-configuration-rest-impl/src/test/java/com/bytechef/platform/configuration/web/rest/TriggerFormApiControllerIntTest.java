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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(TriggerFormApiController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
public class TriggerFormApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private WorkflowService workflowService;

    @MockitoBean
    private JobPrincipalAccessor jobPrincipalAccessor;

    @BeforeEach
    public void beforeEach() {
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(any())).thenReturn(jobPrincipalAccessor);
    }

    @Test
    public void testGetTriggerFormInvalidId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/trigger-form/invalid-id"))
            .andExpect(MockMvcResultMatchers.status()
                .isBadRequest());
    }

    @Test
    public void testGetTriggerFormWorkflowNotFound() throws Exception {
        String id = EncodingUtils.base64EncodeToString("tenant:0:1:workflowUuid:triggerName");

        when(jobPrincipalAccessor.getWorkflowId(anyLong(), anyString())).thenReturn("workflowId");
        when(workflowService.getWorkflow("workflowId")).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/trigger-form/" + id))
            .andExpect(MockMvcResultMatchers.status()
                .isNotFound());
    }

    @Test
    public void testGetTriggerFormNoTriggers() throws Exception {
        String id = EncodingUtils.base64EncodeToString("tenant:0:1:workflowUuid:triggerName");
        Workflow workflow = new Workflow("workflowId", "label: workflow", Workflow.Format.YAML);

        when(jobPrincipalAccessor.getWorkflowId(anyLong(), anyString())).thenReturn("workflowId");
        when(workflowService.getWorkflow("workflowId")).thenReturn(workflow);

        // workflow.getExtensions will return an empty list if not present

        mockMvc.perform(MockMvcRequestBuilders.get("/api/trigger-form/" + id))
            .andExpect(MockMvcResultMatchers.status()
                .isBadRequest());
    }
}
