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

package com.bytechef.automation.datasync.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.dto.DataSyncDTO;
import com.bytechef.automation.datasync.dto.DataSyncDeploymentDTO;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.automation.datasync.web.graphql.config.DataSyncGraphQlConfigurationSharedMocks;
import com.bytechef.automation.datasync.web.graphql.config.DataSyncGraphQlTestConfiguration;
import graphql.ErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {
    DataSyncGraphQlTestConfiguration.class,
    DataSyncGraphQlController.class
})
@GraphQlTest(
    controllers = DataSyncGraphQlController.class,
    properties = "spring.graphql.schema.locations=classpath*:/graphql/")
@DataSyncGraphQlConfigurationSharedMocks
class DataSyncGraphQlControllerTest {

    @Autowired
    private DataSyncFacade dataSyncFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testDataSyncsReturnsMappedRows() {
        DataSync dataSync = new DataSync();

        dataSync.setId(1L);
        dataSync.setName("crm-to-db");
        dataSync.setTitle("CRM to DB");
        dataSync.setWorkspaceId(10L);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.SCHEDULE);
        dataSync.setTriggerParameters(Map.of("expression", "0 9 * * ?"));

        DataSyncElement source = new DataSyncElement(1L, Kind.SOURCE);

        source.setId(5L);
        source.setComponentName("csvFile");
        source.setComponentVersion(1);
        source.setOperationName("read");

        when(dataSyncFacade.getDataSyncs(10L)).thenReturn(List.of(new DataSyncDTO(
            dataSync, 100L, List.of(source), true, 0, null, Instant.parse("2026-09-22T10:15:30Z"),
            "wf-1")));

        graphQlTester.document("""
            query {
                dataSyncs(workspaceId: "10") {
                    id
                    name
                    projectId
                    triggerType
                    triggerParameters
                    draftWorkflowId
                    lastModifiedDate
                    elements { id kind componentName componentVersion operationName }
                }
            }
            """)
            .execute()
            .path("dataSyncs[0].name")
            .entity(String.class)
            .isEqualTo("crm-to-db")
            .path("dataSyncs[0].projectId")
            .entity(String.class)
            .isEqualTo("100")
            .path("dataSyncs[0].triggerType")
            .entity(String.class)
            .isEqualTo("SCHEDULE")
            .path("dataSyncs[0].draftWorkflowId")
            .entity(String.class)
            .isEqualTo("wf-1")
            .path("dataSyncs[0].lastModifiedDate")
            .entity(String.class)
            .isEqualTo("2026-09-22T10:15:30Z")
            .path("dataSyncs[0].elements[0].kind")
            .entity(String.class)
            .isEqualTo("SOURCE");
    }

    @Test
    void testCreateDataSyncPassesNoProject() {
        DataSync dataSync = new DataSync();

        dataSync.setId(2L);
        dataSync.setName("orders");
        dataSync.setTitle("Orders");
        dataSync.setWorkspaceId(10L);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.MANUAL);

        when(dataSyncFacade.createDataSync("Orders", null, 10L, null)).thenReturn(new DataSyncDTO(
            dataSync, 200L, List.of(), true, 0, null, null, "wf-2"));

        graphQlTester.document("""
            mutation {
                createDataSync(input: {title: "Orders", workspaceId: "10"}) { id projectId }
            }
            """)
            .execute()
            .path("createDataSync.projectId")
            .entity(String.class)
            .isEqualTo("200");

        verify(dataSyncFacade).createDataSync("Orders", null, 10L, null);
    }

    @Test
    void testCreateDataSyncPassesProjectId() {
        DataSync dataSync = new DataSync();

        dataSync.setId(3L);
        dataSync.setName("orders-existing");
        dataSync.setTitle("Orders Existing");
        dataSync.setWorkspaceId(10L);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.MANUAL);

        when(dataSyncFacade.createDataSync("Orders Existing", null, 10L, 300L)).thenReturn(new DataSyncDTO(
            dataSync, 300L, List.of(), true, 0, null, null, "wf-3"));

        graphQlTester.document("""
            mutation {
                createDataSync(input: {title: "Orders Existing", workspaceId: "10", projectId: "300"}) {
                    id
                    projectId
                }
            }
            """)
            .execute()
            .path("createDataSync.projectId")
            .entity(String.class)
            .isEqualTo("300");

        verify(dataSyncFacade).createDataSync("Orders Existing", null, 10L, 300L);
    }

    @Test
    void testCreateDataSyncWithoutProjectPassesNull() {
        DataSync dataSync = new DataSync();

        dataSync.setId(4L);
        dataSync.setName("orders-new");
        dataSync.setTitle("Orders New");
        dataSync.setWorkspaceId(10L);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.MANUAL);

        when(dataSyncFacade.createDataSync("Orders New", null, 10L, null)).thenReturn(new DataSyncDTO(
            dataSync, 400L, List.of(), true, 0, null, null, "wf-4"));

        graphQlTester.document("""
            mutation {
                createDataSync(input: {title: "Orders New", workspaceId: "10"}) {
                    id
                    projectId
                }
            }
            """)
            .execute()
            .path("createDataSync.projectId")
            .entity(String.class)
            .isEqualTo("400");

        verify(dataSyncFacade).createDataSync("Orders New", null, 10L, null);
    }

    @Test
    void testDataSyncResolvesProjectWorkflowUuid() {
        DataSync dataSync = new DataSync();

        dataSync.setId(5L);
        dataSync.setName("crm-to-db");
        dataSync.setTitle("CRM to DB");
        dataSync.setWorkspaceId(10L);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setProjectWorkflowUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        dataSync.setTriggerType(TriggerType.MANUAL);

        when(dataSyncFacade.getDataSync(5L)).thenReturn(new DataSyncDTO(
            dataSync, 100L, List.of(), true, 0, null, null, "wf-5"));

        graphQlTester.document("""
            query {
                dataSync(id: "5") {
                    id
                    projectWorkflowUuid
                }
            }
            """)
            .execute()
            .path("dataSync.projectWorkflowUuid")
            .entity(String.class)
            .isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void testDataSyncDeploymentsHaveNoTags() {
        graphQlTester.document("""
            query {
                dataSyncDeployments(workspaceId: "10") {
                    id
                    tags
                }
            }
            """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError)));

        verifyNoInteractions(dataSyncFacade);
    }

    @Test
    void testSetDataSyncElementPassesEveryArgument() {
        DataSyncElement element = new DataSyncElement(1L, Kind.DESTINATION);

        element.setId(9L);
        element.setComponentName("postgresql");
        element.setComponentVersion(1);
        element.setOperationName("insert");

        when(dataSyncFacade.setDataSyncElement(
            eq(1L), eq(Kind.DESTINATION), eq("postgresql"), eq(1), eq("insert"), eq(Map.of("table", "t")), eq(3L)))
                .thenReturn(element);

        graphQlTester.document("""
            mutation {
                setDataSyncElement(input: {
                    dataSyncId: "1", kind: DESTINATION, componentName: "postgresql", componentVersion: 1,
                    operationName: "insert", parameters: {table: "t"}, connectionId: "3"
                }) { id kind }
            }
            """)
            .execute()
            .path("setDataSyncElement.id")
            .entity(String.class)
            .isEqualTo("9");

        verify(dataSyncFacade).setDataSyncElement(1L, Kind.DESTINATION, "postgresql", 1, "insert", Map.of("table", "t"),
            3L);
    }

    @Test
    void testPublishDataSyncIsNotInTheSchema() {
        graphQlTester.document("""
            mutation { publishDataSync(id: "1") }
            """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).singleElement()
                .satisfies(error -> {
                    assertThat(error.getErrorType()).isEqualTo(ErrorType.ValidationError);
                    assertThat(error.getMessage()).contains("publishDataSync");
                }));

        verifyNoInteractions(dataSyncFacade);
    }

    @Test
    void testDataSyncDeploymentsReturnsMappedRows() {
        when(dataSyncFacade.getDataSyncDeployments(10L)).thenReturn(List.of(new DataSyncDeploymentDTO(
            20L, "deploy", 1L, "CRM to DB", 100L, 2, true, 3, "wf-3", TriggerType.SCHEDULE,
            Instant.parse("2026-09-22T10:15:30Z"))));

        graphQlTester.document("""
            query {
                dataSyncDeployments(workspaceId: "10") {
                    id
                    name
                    dataSyncId
                    dataSyncTitle
                    projectId
                    environmentId
                    enabled
                    projectVersion
                    workflowId
                    triggerType
                    lastExecutionDate
                }
            }
            """)
            .execute()
            .path("dataSyncDeployments[0].id")
            .entity(String.class)
            .isEqualTo("20")
            .path("dataSyncDeployments[0].name")
            .entity(String.class)
            .isEqualTo("deploy")
            .path("dataSyncDeployments[0].dataSyncId")
            .entity(String.class)
            .isEqualTo("1")
            .path("dataSyncDeployments[0].enabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("dataSyncDeployments[0].dataSyncTitle")
            .entity(String.class)
            .isEqualTo("CRM to DB")
            .path("dataSyncDeployments[0].projectId")
            .entity(String.class)
            .isEqualTo("100")
            .path("dataSyncDeployments[0].environmentId")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("dataSyncDeployments[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("dataSyncDeployments[0].workflowId")
            .entity(String.class)
            .isEqualTo("wf-3")
            .path("dataSyncDeployments[0].triggerType")
            .entity(String.class)
            .isEqualTo("SCHEDULE")
            .path("dataSyncDeployments[0].lastExecutionDate")
            .entity(String.class)
            .isEqualTo("2026-09-22T10:15:30Z");
    }

    @Test
    void testRunDataSyncDeploymentReturnsJobId() {
        when(dataSyncFacade.runDataSyncDeployment(1L, 20L)).thenReturn(77L);

        graphQlTester.document("""
            mutation { runDataSyncDeployment(id: "1", projectDeploymentId: "20") }
            """)
            .execute()
            .path("runDataSyncDeployment")
            .entity(String.class)
            .isEqualTo("77");
    }
}
