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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.configuration.dto.ClusterElementOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(WorkflowNodeOutputApiController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
class WorkflowNodeOutputApiControllerIntTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final long DEVELOPMENT_ENVIRONMENT_ID = 0L;
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
    private static final String WORKFLOW_ID = "workflow1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowNodeOutputApiController workflowNodeOutputApiController;

    @Autowired
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetClusterElementOutputPassesTestOutputResponseThrough() {
        stubClusterElementOutput(true);

        getClusterElementOutput()
            .jsonPath("$.testOutputResponse")
            .isEqualTo(true)
            .jsonPath("$.workflowNodeName")
            .isEqualTo("hubspot_1")
            .jsonPath("$.clusterElementDefinition.name")
            .isEqualTo("createContact");
    }

    @Test
    void testGetClusterElementOutputPassesMissingTestOutputResponseThrough() {
        stubClusterElementOutput(false);

        getClusterElementOutput()
            .jsonPath("$.testOutputResponse")
            .isEqualTo(false)
            .jsonPath("$.workflowNodeName")
            .isEqualTo("hubspot_1")
            .jsonPath("$.clusterElementDefinition.name")
            .isEqualTo("createContact");
    }

    @Test
    void testGetClusterElementOutputReturnsNotFoundWhenThereIsNoOutput() {
        webTestClient
            .get()
            .uri(
                "/internal/workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}" +
                    "/{clusterElementWorkflowNodeName}/outputs?environmentId={environmentId}",
                WORKFLOW_ID, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    private WebTestClient.BodyContentSpec getClusterElementOutput() {
        return webTestClient
            .get()
            .uri(
                "/internal/workflows/{id}/workflow-nodes/{workflowNodeName}/cluster-elements/{clusterElementType}" +
                    "/{clusterElementWorkflowNodeName}/outputs?environmentId={environmentId}",
                WORKFLOW_ID, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody();
    }

    private void stubClusterElementOutput(boolean testOutputResponse) {
        ClusterElementDefinition clusterElementDefinition = new ClusterElementDefinition(
            ComponentDsl.<Object>clusterElement("createContact")
                .title("Create Contact")
                .type(new ClusterElementType("TOOLS", "tools", "Tools"))
                .object(Object::new),
            "hubspot", 1, null);

        ClusterElementOutputDTO clusterElementOutputDTO = new ClusterElementOutputDTO(
            clusterElementDefinition, null, null, Map.of("id", "contact-42"), false, testOutputResponse, "hubspot_1");

        when(workflowNodeOutputFacade.getClusterElementOutput(
            WORKFLOW_ID, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID)).thenReturn(clusterElementOutputDTO);
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsUsesTheConfinedPrincipalEnvironmentForEvictionAndRead() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ENVIRONMENT_ID));

        workflowNodeOutputApiController.getPreviousWorkflowNodeOutputs(
            WORKFLOW_ID, DEVELOPMENT_ENVIRONMENT_ID, "node_1");

        verify(workflowNodeOutputFacade).checkWorkflowCache(WORKFLOW_ID, "node_1", PRODUCTION_ENVIRONMENT_ID);
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            WORKFLOW_ID, "node_1", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsUsesTheSessionPrincipalRequestedEnvironmentForEvictionAndRead() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        workflowNodeOutputApiController.getPreviousWorkflowNodeOutputs(
            WORKFLOW_ID, DEVELOPMENT_ENVIRONMENT_ID, "node_1");

        verify(workflowNodeOutputFacade).checkWorkflowCache(WORKFLOW_ID, "node_1", DEVELOPMENT_ENVIRONMENT_ID);
        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs(
            WORKFLOW_ID, "node_1", DEVELOPMENT_ENVIRONMENT_ID);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId) {
            super(environmentId, new User("connected-user-1", "", List.of()));
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.PLATFORM_API_KEY;
        }

        @Override
        public @Nullable PlatformType getPlatformType() {
            return null;
        }
    }
}
