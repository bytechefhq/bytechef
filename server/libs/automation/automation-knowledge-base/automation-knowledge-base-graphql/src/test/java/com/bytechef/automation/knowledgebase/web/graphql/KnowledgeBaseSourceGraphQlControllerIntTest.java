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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.dto.UpdateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseSourceService;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link KnowledgeBaseSourceGraphQlController}. Mocks the facade and verifies each query/mutation
 * routes to the right facade method with the right args, plus that the admin-only {@code refreshKnowledgeBaseSource}
 * mutation rejects anonymous callers.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseSourceGraphQlController.class
})
@GraphQlTest(
    controllers = KnowledgeBaseSourceGraphQlController.class,
    properties = {
        "bytechef.ai.knowledge-base.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseSourceGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseSourceService knowledgeBaseSourceService;

    @Autowired
    private WorkspaceKnowledgeBaseSourceFacade workspaceKnowledgeBaseSourceFacade;

    @Autowired
    private WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testKnowledgeBaseSourceById() {
        KnowledgeBaseSource source = createMockSource(1L, 10L, "demo");

        when(knowledgeBaseSourceService.fetch(1L)).thenReturn(Optional.of(source));

        graphQlTester
            .document("""
                query {
                    knowledgeBaseSource(id: "1") {
                        id
                        workspaceId
                        name
                        status
                        enabled
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseSource.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("knowledgeBaseSource.workspaceId")
            .entity(String.class)
            .isEqualTo("10")
            .path("knowledgeBaseSource.name")
            .entity(String.class)
            .isEqualTo("demo")
            .path("knowledgeBaseSource.status")
            .entity(String.class)
            .isEqualTo("READY")
            .path("knowledgeBaseSource.enabled")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(knowledgeBaseSourceService).fetch(1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testKnowledgeBaseSourceByIdNotFound() {
        when(knowledgeBaseSourceService.fetch(99L)).thenReturn(Optional.empty());

        graphQlTester
            .document("""
                query {
                    knowledgeBaseSource(id: "99") {
                        id
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseSource")
            .valueIsNull();

        verify(knowledgeBaseSourceService).fetch(99L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testKnowledgeBaseSourcesByWorkspaceWithoutFilter() {
        List<KnowledgeBaseSource> sources = List.of(
            createMockSource(1L, 10L, "first"),
            createMockSource(2L, 10L, "second"));

        when(workspaceKnowledgeBaseSourceService.getAllSourcesByWorkspaceId(10L, 1L)).thenReturn(sources);

        graphQlTester
            .document("""
                query {
                    knowledgeBaseSources(workspaceId: "10", environmentId: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseSources")
            .entityList(Object.class)
            .hasSize(2);

        verify(workspaceKnowledgeBaseSourceService).getAllSourcesByWorkspaceId(10L, 1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testKnowledgeBaseSourcesByWorkspaceWithEnabledFilter() {
        List<KnowledgeBaseSource> sources = List.of(createMockSource(1L, 10L, "first"));

        when(workspaceKnowledgeBaseSourceService.getAllEnabledSourcesByWorkspaceId(10L, 1L)).thenReturn(sources);

        graphQlTester
            .document("""
                query {
                    knowledgeBaseSources(workspaceId: "10", environmentId: "1", filter: { enabled: true }) {
                        id
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseSources")
            .entityList(Object.class)
            .hasSize(1);

        verify(workspaceKnowledgeBaseSourceService).getAllEnabledSourcesByWorkspaceId(10L, 1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testCreateKnowledgeBaseSource() {
        KnowledgeBaseSource source = createMockSource(1L, 10L, "new-source");

        source.setWorkflowId("wf-uuid-123");

        when(workspaceKnowledgeBaseSourceFacade.create(eq(10L), any(CreateKnowledgeBaseSourceInput.class)))
            .thenReturn(source);

        graphQlTester
            .document("""
                mutation {
                    createKnowledgeBaseSource(input: {
                        workspaceId: "10",
                        knowledgeBaseId: "20",
                        name: "new-source",
                        sourceComponentName: "github",
                        sourceComponentVersion: 1,
                        sourceClusterElementName: "issuesReader",
                        cadence: "0 0 * * * *"
                    }) {
                        id
                        workflowId
                    }
                }
                """)
            .execute()
            .path("createKnowledgeBaseSource.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createKnowledgeBaseSource.workflowId")
            .entity(String.class)
            .isEqualTo("wf-uuid-123");

        ArgumentCaptor<CreateKnowledgeBaseSourceInput> captor =
            ArgumentCaptor.forClass(CreateKnowledgeBaseSourceInput.class);

        verify(workspaceKnowledgeBaseSourceFacade).create(eq(10L), captor.capture());

        CreateKnowledgeBaseSourceInput captured = captor.getValue();

        assertThat(captured.name()).isEqualTo("new-source");
        assertThat(captured.knowledgeBaseId()).isEqualTo(20L);
        assertThat(captured.sourceComponentName()).isEqualTo("github");
        assertThat(captured.sourceComponentVersion()).isEqualTo(1);
        assertThat(captured.sourceClusterElementName()).isEqualTo("issuesReader");
        assertThat(captured.cadence()).isEqualTo("0 0 * * * *");
        assertThat(captured.parameters()).isNull();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testCreateKnowledgeBaseSourceWithParameters() {
        KnowledgeBaseSource source = createMockSource(2L, 10L, "with-params");

        when(workspaceKnowledgeBaseSourceFacade.create(eq(10L), any(CreateKnowledgeBaseSourceInput.class)))
            .thenReturn(source);

        graphQlTester
            .document("""
                mutation {
                    createKnowledgeBaseSource(input: {
                        workspaceId: "10",
                        knowledgeBaseId: "20",
                        name: "with-params",
                        sourceComponentName: "airtable",
                        sourceComponentVersion: 1,
                        sourceClusterElementName: "selectRecords",
                        cadence: "0 0 * * * *",
                        parameters: { baseId: "appAbc", tableId: "tblXyz" }
                    }) {
                        id
                    }
                }
                """)
            .execute()
            .path("createKnowledgeBaseSource.id")
            .entity(String.class)
            .isEqualTo("2");

        ArgumentCaptor<CreateKnowledgeBaseSourceInput> captor =
            ArgumentCaptor.forClass(CreateKnowledgeBaseSourceInput.class);

        verify(workspaceKnowledgeBaseSourceFacade).create(eq(10L), captor.capture());

        CreateKnowledgeBaseSourceInput captured = captor.getValue();

        @SuppressWarnings("unchecked")
        Map<String, Object> capturedParameters = (Map<String, Object>) captured.parameters();

        assertThat(capturedParameters)
            .isNotNull()
            .containsEntry("baseId", "appAbc")
            .containsEntry("tableId", "tblXyz");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testUpdateKnowledgeBaseSource() {
        KnowledgeBaseSource source = createMockSource(1L, 10L, "renamed");

        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(1L))
            .thenReturn(Optional.of(10L));
        when(workspaceKnowledgeBaseSourceFacade.update(eq(10L), eq(1L), any(UpdateKnowledgeBaseSourceInput.class)))
            .thenReturn(source);

        graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseSource(id: "1", input: { name: "renamed", cadence: "0 */5 * * * *" }) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseSource.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateKnowledgeBaseSource.name")
            .entity(String.class)
            .isEqualTo("renamed");

        ArgumentCaptor<UpdateKnowledgeBaseSourceInput> captor =
            ArgumentCaptor.forClass(UpdateKnowledgeBaseSourceInput.class);

        verify(workspaceKnowledgeBaseSourceFacade).update(eq(10L), eq(1L), captor.capture());

        UpdateKnowledgeBaseSourceInput captured = captor.getValue();

        assertThat(captured.name()).isEqualTo("renamed");
        assertThat(captured.cadence()).isEqualTo("0 */5 * * * *");
        assertThat(captured.enabled()).isNull();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeleteKnowledgeBaseSource() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(1L))
            .thenReturn(Optional.of(10L));

        graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBaseSource(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBaseSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceKnowledgeBaseSourceFacade).delete(10L, 1L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testRefreshKnowledgeBaseSourceReturnsJobId() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(1L))
            .thenReturn(Optional.of(10L));
        when(workspaceKnowledgeBaseSourceFacade.refreshNow(10L, 1L)).thenReturn(42L);

        graphQlTester
            .document("""
                mutation {
                    refreshKnowledgeBaseSource(id: "1")
                }
                """)
            .execute()
            .path("refreshKnowledgeBaseSource")
            .entity(String.class)
            .isEqualTo("42");

        verify(workspaceKnowledgeBaseSourceFacade).refreshNow(10L, 1L);
    }

    // The admin-only guard on refreshKnowledgeBaseSource moved from this controller onto
    // WorkspaceKnowledgeBaseSourceFacadeImpl.refreshNow; its presence is pinned by
    // WorkspaceKnowledgeBaseSourceFacadeAuthorizationTest in the service module.

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testSetKnowledgeBaseSourceEnabled() {
        KnowledgeBaseSource source = createMockSource(1L, 10L, "demo");

        source.setEnabled(false);

        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(1L))
            .thenReturn(Optional.of(10L));
        when(knowledgeBaseSourceService.get(1L)).thenReturn(source);

        graphQlTester
            .document("""
                mutation {
                    setKnowledgeBaseSourceEnabled(id: "1", enabled: false) {
                        id
                        enabled
                    }
                }
                """)
            .execute()
            .path("setKnowledgeBaseSourceEnabled.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("setKnowledgeBaseSourceEnabled.enabled")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(workspaceKnowledgeBaseSourceFacade).setEnabled(10L, 1L, false);
        verify(knowledgeBaseSourceService).get(1L);
    }

    private KnowledgeBaseSource createMockSource(Long id, Long workspaceId, String name) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setId(id);
        source.setName(name);
        source.setKnowledgeBaseId(20L);
        source.setSourceComponentName("github");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("issuesReader");
        source.setCadence("0 0 * * * *");
        source.setStatus(KnowledgeBaseSourceStatus.READY);
        source.setEnabled(true);
        source.setWorkspaceId(workspaceId);

        // The mutation resolvers still resolve the acting workspace from the service; the workspaceId GraphQL field
        // is read straight off the entity column.
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(id))
            .thenReturn(Optional.of(workspaceId));

        return source;
    }
}
