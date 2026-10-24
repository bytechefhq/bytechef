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

package com.bytechef.automation.datasync.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.listener.ProjectContentContributor;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.datasync.config.AutomationDataSyncIntTestConfiguration;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.automation.datasync.repository.DataSyncElementRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import com.bytechef.automation.datasync.service.DataSyncService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * {@link DataSyncProjectContentContributor} is what carries a project's data syncs through duplicate, export/import and
 * git pull; {@code ProjectGitFacadeTest} pins that the git operations call it.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationDataSyncIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class DataSyncProjectContentContributorIntTest {

    @Autowired
    private DataSyncElementRepository dataSyncElementRepository;

    @Autowired
    private DataSyncFacade dataSyncFacade;

    @Autowired
    private DataSyncRepository dataSyncRepository;

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private List<ProjectContentContributor> projectContentContributors;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectContentContributor dataSyncContentContributor;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        workspaceId = workspace.getId();

        dataSyncContentContributor = projectContentContributors.stream()
            .filter(contributor -> contributor instanceof DataSyncProjectContentContributor)
            .findFirst()
            .orElseThrow();
    }

    @AfterEach
    void afterEach() {
        dataSyncElementRepository.deleteAll();
        dataSyncRepository.deleteAll();
        projectWorkflowRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testContentDirectoryIsDataSyncs() {
        assertThat(dataSyncContentContributor.getContentDirectory()).isEqualTo("data-syncs/");
    }

    @Test
    void testDuplicateCopiesSyncsWithConnections() {
        long sourceProjectId = createProject("Source");
        DataSync ordersDataSync = createConfiguredDataSync("Orders", sourceProjectId);

        dataSyncFacade.updateDataSyncTrigger(
            ordersDataSync.getId(), TriggerType.SCHEDULE, Map.of("expression", "0 0 6 * * ?", "timezone", "UTC"));

        createConfiguredDataSync("Customers", sourceProjectId);

        long duplicateProjectId = createProject("Source (copy)");

        dataSyncContentContributor.onProjectDuplicated(sourceProjectId, duplicateProjectId);

        List<DataSync> copiedDataSyncs = dataSyncService.getProjectDataSyncs(duplicateProjectId);

        assertThat(copiedDataSyncs).extracting(DataSync::getTitle)
            .containsExactlyInAnyOrder("Orders", "Customers");
        assertThat(copiedDataSyncs).extracting(DataSync::getName)
            .containsExactlyInAnyOrder("orders-2", "customers-2");
        assertThat(copiedDataSyncs).extracting(DataSync::getWorkspaceId)
            .containsOnly(workspaceId);

        DataSync copiedOrdersDataSync = findByTitle(copiedDataSyncs, "Orders");

        assertThat(copiedOrdersDataSync.getTriggerType()).isEqualTo(TriggerType.SCHEDULE);
        assertThat(elements(copiedOrdersDataSync.getId()))
            .extracting(DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId)
            .containsExactly(
                tuple(Kind.SOURCE, "csvFile", 1L), tuple(Kind.DESTINATION, "postgresql", 2L),
                tuple(Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, null));
        assertThat(dataSyncService.getProjectDataSyncs(sourceProjectId)).extracting(DataSync::getName)
            .containsExactlyInAnyOrder("orders", "customers");
    }

    @Test
    void testExportWritesOneFilePerSyncWithoutConnectionIds() {
        long projectId = createProject("Source");

        createConfiguredDataSync("Orders", projectId);
        createConfiguredDataSync("Customers", projectId);

        Map<String, byte[]> files = dataSyncContentContributor.exportProjectContent(projectId);

        assertThat(files).containsOnlyKeys("data-syncs/orders.json", "data-syncs/customers.json");

        String ordersJson = string(files.get("data-syncs/orders.json"));

        assertThat(ordersJson).doesNotContain("connectionId")
            .doesNotContain("connection");
        Map<String, Object> ordersDocument = new HashMap<>(JsonUtils.readMap(ordersJson));

        assertThat(ordersDocument).containsEntry("name", "orders")
            .containsEntry("title", "Orders");
    }

    @Test
    void testImportRecreatesSyncsWithoutConnections() {
        long sourceProjectId = createProject("Source");

        createConfiguredDataSync("Orders", sourceProjectId);

        Map<String, byte[]> files = new HashMap<>(dataSyncContentContributor.exportProjectContent(sourceProjectId));

        // Neither is a data sync file, and neither is valid: reading either would throw.
        files.put("data-syncs/nested/ignored.json", bytes("not json"));
        files.put("data-syncs/readme.txt", bytes("not json"));

        long importedProjectId = createProject("Imported");

        dataSyncContentContributor.importProjectContent(importedProjectId, workspaceId, files);

        List<DataSync> importedDataSyncs = dataSyncService.getProjectDataSyncs(importedProjectId);

        assertThat(importedDataSyncs).singleElement()
            .satisfies(dataSync -> {
                assertThat(dataSync.getTitle()).isEqualTo("Orders");
                assertThat(dataSync.getName()).isEqualTo("orders-2");
                assertThat(dataSync.getWorkspaceId()).isEqualTo(workspaceId);
            });
        assertThat(elements(importedDataSyncs.getFirst()
            .getId()))
                .extracting(
                    DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId)
                .containsExactly(
                    tuple(Kind.SOURCE, "csvFile", null), tuple(Kind.DESTINATION, "postgresql", null),
                    tuple(Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, null));
    }

    @Test
    void testPullUpdatesByNameThenUniqueTitle() {
        long otherProjectId = createProject("Other");

        // Takes the name "customers" in the workspace, so the project's own Customers sync becomes "customers-2".
        createConfiguredDataSync("Customers", otherProjectId);

        long projectId = createProject("Project");
        DataSync ordersDataSync = createConfiguredDataSync("Orders", projectId);
        DataSync customersDataSync = createConfiguredDataSync("Customers", projectId);

        assertThat(customersDataSync.getName()).isEqualTo("customers-2");

        Map<String, Object> ordersDocument = exportDocument(ordersDataSync.getId());

        ordersDocument.put("description", "pulled by name");

        Map<String, Object> customersDocument = exportDocument(customersDataSync.getId());

        customersDocument.put("name", "customers");
        customersDocument.put("description", "pulled by title");

        dataSyncContentContributor.pullProjectContent(
            projectId,
            Map.of(
                "data-syncs/orders.json", bytes(JsonUtils.write(ordersDocument)),
                "data-syncs/customers.json", bytes(JsonUtils.write(customersDocument))));

        assertThat(dataSyncService.getProjectDataSyncs(projectId))
            .extracting(DataSync::getId, DataSync::getDescription)
            .containsExactlyInAnyOrder(
                tuple(ordersDataSync.getId(), "pulled by name"), tuple(customersDataSync.getId(), "pulled by title"));
        assertThat(dataSyncService.getProjectDataSyncs(otherProjectId)).singleElement()
            .satisfies(dataSync -> assertThat(dataSync.getDescription()).isNull());
    }

    @Test
    void testPullKeepsConnectionsOfUnchangedElements() {
        long projectId = createProject("Project");
        DataSync ordersDataSync = createConfiguredDataSync("Orders", projectId);

        Map<String, Object> document = exportDocument(ordersDataSync.getId());

        document.put(
            "elements",
            List.of(
                exportedElement("SOURCE", "csvFile", 1, "read", Map.of("delimiter", ";")),
                exportedElement("DESTINATION", "mysql", 1, "insert", Map.of())));

        dataSyncContentContributor.pullProjectContent(
            projectId, Map.of("data-syncs/orders.json", bytes(JsonUtils.write(document))));

        assertThat(elements(ordersDataSync.getId()))
            .extracting(
                DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId,
                DataSyncElement::getParameters)
            .as("an unchanged operation keeps its connection, a changed one loses it, a missing kind is deleted")
            .containsExactly(
                tuple(Kind.SOURCE, "csvFile", 1L, Map.of("delimiter", ";")),
                tuple(Kind.DESTINATION, "mysql", null, Map.of()));
    }

    @Test
    void testPullImportsUnmatchedFiles() {
        long projectId = createProject("Project");
        DataSync ordersDataSync = createConfiguredDataSync("Orders", projectId);

        Map<String, Object> newDocument = exportDocument(ordersDataSync.getId());

        newDocument.put("name", "invoices");
        newDocument.put("title", "Invoices");

        dataSyncContentContributor.pullProjectContent(
            projectId, Map.of("data-syncs/invoices.json", bytes(JsonUtils.write(newDocument))));

        List<DataSync> projectDataSyncs = dataSyncService.getProjectDataSyncs(projectId);

        assertThat(projectDataSyncs).extracting(DataSync::getTitle)
            .containsExactlyInAnyOrder("Orders", "Invoices");

        DataSync invoicesDataSync = findByTitle(projectDataSyncs, "Invoices");

        assertThat(invoicesDataSync.getName()).isEqualTo("invoices");
        assertThat(elements(invoicesDataSync.getId())).extracting(DataSyncElement::getConnectionId)
            .containsOnlyNulls();
        assertThat(elements(ordersDataSync.getId())).extracting(DataSyncElement::getConnectionId)
            .containsExactly(1L, 2L, null);
    }

    @Test
    void testPullLeavesSyncsWithoutAFileAlone() {
        long projectId = createProject("Project");
        DataSync ordersDataSync = createConfiguredDataSync("Orders", projectId);
        DataSync customersDataSync = createConfiguredDataSync("Customers", projectId);

        Map<String, Object> ordersDocument = exportDocument(ordersDataSync.getId());

        ordersDocument.put("elements", List.of());

        dataSyncContentContributor.pullProjectContent(
            projectId, Map.of("data-syncs/orders.json", bytes(JsonUtils.write(ordersDocument))));

        assertThat(dataSyncService.getProjectDataSyncs(projectId)).extracting(DataSync::getId)
            .containsExactlyInAnyOrder(ordersDataSync.getId(), customersDataSync.getId());
        assertThat(elements(ordersDataSync.getId())).isEmpty();
        assertThat(elements(customersDataSync.getId()))
            .extracting(DataSyncElement::getKind, DataSyncElement::getConnectionId)
            .containsExactly(tuple(Kind.SOURCE, 1L), tuple(Kind.DESTINATION, 2L), tuple(Kind.PROCESSOR, null));
        assertThat(dataSyncService.getDataSync(customersDataSync.getId())
            .getTitle()).isEqualTo("Customers");
    }

    private DataSync createConfiguredDataSync(String title, long projectId) {
        DataSync dataSync = dataSyncFacade.createDataSync(title, null, workspaceId, projectId)
            .dataSync();
        long id = dataSync.getId();

        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "csvFile", 1, "read", Map.of("delimiter", ","), 1L);
        dataSyncFacade.setDataSyncElement(id, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);
        dataSyncFacade.setDataSyncElement(
            id, Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, DataSyncElement.PROCESSOR_COMPONENT_VERSION,
            DataSyncElement.PROCESSOR_OPERATION_NAME, Map.of("mappings", List.of()), null);

        return dataSyncService.getDataSync(id);
    }

    private long createProject(String name) {
        Project project = new Project();

        project.setName(name);
        project.setWorkspaceId(workspaceId);

        return Objects.requireNonNull(projectService.create(project)
            .getId());
    }

    private List<DataSyncElement> elements(long dataSyncId) {
        return dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(dataSyncId);
    }

    private Map<String, Object> exportDocument(long dataSyncId) {
        return new HashMap<>(JsonUtils.readMap(dataSyncFacade.exportDataSync(dataSyncId)));
    }

    private static Map<String, Object> exportedElement(
        String kind, String componentName, int componentVersion, String operationName, Map<String, Object> parameters) {

        return Map.of(
            "kind", kind, "componentName", componentName, "componentVersion", componentVersion, "operationName",
            operationName, "parameters", parameters);
    }

    private static DataSync findByTitle(List<DataSync> dataSyncs, String title) {
        return dataSyncs.stream()
            .filter(dataSync -> title.equals(dataSync.getTitle()))
            .findFirst()
            .orElseThrow();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
