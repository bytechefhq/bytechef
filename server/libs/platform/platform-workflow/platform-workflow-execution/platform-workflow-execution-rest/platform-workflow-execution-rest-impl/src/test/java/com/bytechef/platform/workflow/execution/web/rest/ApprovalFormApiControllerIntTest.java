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

package com.bytechef.platform.workflow.execution.web.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.workflow.execution.facade.ApprovalFormFacade;
import com.bytechef.platform.workflow.execution.web.rest.model.ApprovalFormModel;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApprovalFormApiController.class,
    ApprovalFormApiControllerIntTest.ApprovalFormApiControllerIntTestConfiguration.class
})
@WebMvcTest(controllers = ApprovalFormApiController.class, properties = "bytechef.coordinator.enabled=true")
class ApprovalFormApiControllerIntTest {

    private static final String APPROVAL_FORM_PATH = "/internal/approval-form/{id}";
    private static final String TOKEN = "v1.1792298229.payload.signature";

    @MockitoBean
    private ApprovalFormFacade approvalFormFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetApprovalFormReturnsTheForm() throws Exception {
        when(approvalFormFacade.fetchApprovalForm(TOKEN)).thenReturn(
            Optional.of(Map.of("formTitle", "Approve the invoice")));

        mockMvc.perform(get(APPROVAL_FORM_PATH, TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formTitle").value("Approve the invoice"));
    }

    @Test
    void testGetApprovalFormIsNotFoundWhenTheApprovalIsNoLongerPending() throws Exception {
        when(approvalFormFacade.fetchApprovalForm(TOKEN)).thenReturn(Optional.empty());

        mockMvc.perform(get(APPROVAL_FORM_PATH, TOKEN))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetApprovalFormIsBadRequestForAnInvalidToken() throws Exception {
        when(approvalFormFacade.fetchApprovalForm(TOKEN)).thenThrow(
            new IllegalArgumentException("Invalid approval form id"));

        mockMvc.perform(get(APPROVAL_FORM_PATH, TOKEN))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetApprovalFormIsServerErrorWhenLoadingTheFormFails() throws Exception {
        when(approvalFormFacade.fetchApprovalForm(TOKEN)).thenThrow(
            new RuntimeException(
                new IllegalStateException("cannot execute SELECT FOR UPDATE in a read-only transaction")));

        mockMvc.perform(get(APPROVAL_FORM_PATH, TOKEN))
            .andExpect(status().isInternalServerError());
    }

    @TestConfiguration
    static class ApprovalFormApiControllerIntTestConfiguration {

        @Bean
        ConversionService conversionService() {
            GenericConversionService conversionService = new GenericConversionService();

            conversionService.addConverter(
                Map.class, ApprovalFormModel.class,
                approvalForm -> new ApprovalFormModel().formTitle((String) approvalForm.get("formTitle")));

            return conversionService;
        }
    }
}
