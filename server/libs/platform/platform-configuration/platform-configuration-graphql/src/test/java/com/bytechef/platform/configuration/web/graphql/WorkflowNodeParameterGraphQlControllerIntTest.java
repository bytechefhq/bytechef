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

package com.bytechef.platform.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class,
    WorkflowNodeParameterGraphQlController.class
})
@GraphQlTest(
    controllers = WorkflowNodeParameterGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
public class WorkflowNodeParameterGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @Test
    void testClusterElementMissingRequiredPropertiesWithMissingProperties() {
        when(workflowNodeParameterFacade.getClusterElementMissingRequiredProperties(
            anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Set.of("fieldA", "fieldB"));

        this.graphQlTester
            .document("""
                query {
                    clusterElementMissingRequiredProperties(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementTypeName: "PROCESSOR"
                        clusterElementWorkflowNodeName: "script_1"
                    )
                }
                """)
            .execute()
            .path("clusterElementMissingRequiredProperties")
            .entityList(String.class)
            .hasSize(2);
    }

    @Test
    void testClusterElementMissingRequiredPropertiesWithNoMissingProperties() {
        when(workflowNodeParameterFacade.getClusterElementMissingRequiredProperties(
            anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Set.of());

        this.graphQlTester
            .document("""
                query {
                    clusterElementMissingRequiredProperties(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementTypeName: "SOURCE"
                        clusterElementWorkflowNodeName: "csv-file_1"
                    )
                }
                """)
            .execute()
            .path("clusterElementMissingRequiredProperties")
            .entityList(String.class)
            .hasSize(0);
    }

    @Test
    void testWorkflowNodeMissingRequiredPropertiesWithMissingProperties() {
        when(workflowNodeParameterFacade.getWorkflowNodeMissingRequiredProperties(anyString(), anyString()))
            .thenReturn(Set.of("requiredField1", "nested.requiredField2"));

        this.graphQlTester
            .document("""
                query {
                    workflowNodeMissingRequiredProperties(
                        workflowId: "workflow-456"
                        workflowNodeName: "http-request_1"
                    )
                }
                """)
            .execute()
            .path("workflowNodeMissingRequiredProperties")
            .entityList(String.class)
            .hasSize(2);
    }

    @Test
    void testWorkflowNodeMissingRequiredPropertiesWithNoMissingProperties() {
        when(workflowNodeParameterFacade.getWorkflowNodeMissingRequiredProperties(anyString(), anyString()))
            .thenReturn(Set.of());

        this.graphQlTester
            .document("""
                query {
                    workflowNodeMissingRequiredProperties(
                        workflowId: "workflow-456"
                        workflowNodeName: "http-request_1"
                    )
                }
                """)
            .execute()
            .path("workflowNodeMissingRequiredProperties")
            .entityList(String.class)
            .hasSize(0);
    }
}
