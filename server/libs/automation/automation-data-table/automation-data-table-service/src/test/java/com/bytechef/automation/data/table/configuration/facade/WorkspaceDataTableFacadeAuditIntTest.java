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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.data.table.config.AutomationDataTableIntTestConfiguration;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.audit.DataTableAuditEvents;
import com.bytechef.platform.data.table.audit.DataTableAuditMapper;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationDataTableIntTestConfiguration.class, WorkspaceDataTableFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = ProjectOwnershipService.class)
class WorkspaceDataTableFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final List<String> BASE_NAMES = List.of("audited", "audited_copy");
    private static final List<ColumnSpec> COLUMN_SPECS = List.of(new ColumnSpec("title", ColumnType.STRING));
    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 9101;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void beforeEach() {
        dropTestTables();

        jdbcTemplate.update(
            "INSERT INTO workspace " +
                "(id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            WORKSPACE_ID, "data-table-audit-workspace");

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        dropTestTables();

        jdbcTemplate.update("DELETE FROM workspace_data_table WHERE workspace_id = ?", WORKSPACE_ID);
        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateTableRecordsTheTableAndWorkspace() {
        workspaceDataTableFacade.createTable("audited", null, COLUMN_SPECS, WORKSPACE_ID, ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("baseName", "audited")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testCreateOfAnExistingTableRecordsAnError() {
        workspaceDataTableFacade.createTable("audited", null, COLUMN_SPECS, WORKSPACE_ID, ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        assertThatThrownBy(
            () -> workspaceDataTableFacade.createTable("audited", null, COLUMN_SPECS, WORKSPACE_ID, ENVIRONMENT_ID))
                .isInstanceOf(BadSqlGrammarException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", BadSqlGrammarException.class.getName())
            .containsEntry("baseName", "audited")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID));
    }

    @Test
    void testDuplicateTableRecordsTheSourceTableAndNewName() {
        workspaceDataTableFacade.createTable("audited", null, COLUMN_SPECS, WORKSPACE_ID, ENVIRONMENT_ID);

        long dataTableId = dataTableService.getIdByBaseName("audited");

        persistenceAuditEventRepository.deleteAll();

        workspaceDataTableFacade.duplicateTable(dataTableId, "audited_copy", ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_DUPLICATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("sourceDataTableId", String.valueOf(dataTableId))
            .containsEntry("newBaseName", "audited_copy")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testDuplicateOfAnUnknownTableRecordsAnError() {
        assertThatThrownBy(
            () -> workspaceDataTableFacade.duplicateTable(Long.MAX_VALUE, "audited_copy", ENVIRONMENT_ID))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_DUPLICATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("sourceDataTableId", String.valueOf(Long.MAX_VALUE))
            .containsEntry("newBaseName", "audited_copy")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID));
    }

    private void dropTestTables() {
        for (String baseName : BASE_NAMES) {
            dataTableService.dropTable(baseName, ENVIRONMENT_ID);
        }
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class,
        DataTableAuditMapper.class
    })
    static class AuditConfiguration {
    }
}
