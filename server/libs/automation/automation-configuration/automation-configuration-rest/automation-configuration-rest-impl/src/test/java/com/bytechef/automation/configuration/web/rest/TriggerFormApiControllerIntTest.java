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

package com.bytechef.automation.configuration.web.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestTestConfiguration;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.category.service.CategoryService;
import org.junit.jupiter.api.Test;
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
@ContextConfiguration(classes = AutomationConfigurationRestTestConfiguration.class)
@WebMvcTest(TriggerFormApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
public class TriggerFormApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProjectCategoryFacade projectCategoryFacade;

    @MockitoBean
    private ProjectDeploymentFacade projectDeploymentFacade;

    @MockitoBean
    private ProjectFacade projectFacade;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectTagFacade projectTagFacade;

    @MockitoBean
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @Test
    public void testGetTriggerFormInvalidId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/trigger-form/invalid-id"))
            .andExpect(MockMvcResultMatchers.status()
                .isBadRequest());
    }

    @Test
    public void testGetTriggerFormNoTriggers() throws Exception {
        String id = EncodingUtils.base64EncodeToString("tenant:0:1:workflowUuid:triggerName");
        Workflow workflow = new Workflow("workflowId", "label: workflow", Workflow.Format.YAML);

        when(projectWorkflowService.getProjectWorkflowWorkflowId(anyLong(), anyString())).thenReturn("workflowId");
        when(workflowService.getWorkflow("workflowId")).thenReturn(workflow);

        // workflow.getExtensions will return an empty list if not present

        mockMvc.perform(MockMvcRequestBuilders.get("/internal/trigger-form/" + id))
            .andExpect(MockMvcResultMatchers.status()
                .isBadRequest());
    }
}
