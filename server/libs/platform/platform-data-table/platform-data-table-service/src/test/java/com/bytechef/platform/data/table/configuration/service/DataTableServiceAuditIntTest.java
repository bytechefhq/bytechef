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

package com.bytechef.platform.data.table.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        DataTableIntTestConfiguration.class, DataTableServiceAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
class DataTableServiceAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long ENVIRONMENT_ID = 0;
    private static final List<String> BASE_NAMES = List.of("audited", "audited_renamed", "missing");

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        dropTestTables();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        dropTestTables();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testAddColumnRecordsTheColumn() {
        createAuditedTable();

        dataTableService.addColumn("audited", new ColumnSpec("amount", ColumnType.NUMBER), ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_COLUMN_ADDED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("baseName", "audited")
            .containsEntry("columnName", "amount")
            .containsEntry("columnType", ColumnType.NUMBER.name())
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testAddColumnToAnUnknownTableRecordsAnError() {
        ColumnSpec columnSpec = new ColumnSpec("amount", ColumnType.NUMBER);

        assertThatThrownBy(() -> dataTableService.addColumn("missing", columnSpec, ENVIRONMENT_ID))
            .isInstanceOf(BadSqlGrammarException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_COLUMN_ADDED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", BadSqlGrammarException.class.getName())
            .containsEntry("baseName", "missing")
            .containsEntry("columnName", "amount")
            .containsEntry("columnType", ColumnType.NUMBER.name())
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID));
    }

    @Test
    void testDropTableRecordsTheTable() {
        createAuditedTable();

        boolean dropped = dataTableService.dropTable("audited", ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_DELETED);

        assertThat(dropped).isTrue();

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("baseName", "audited")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testRemoveColumnRecordsTheColumn() {
        createAuditedTable();

        dataTableService.removeColumn("audited", "title", ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_COLUMN_REMOVED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("baseName", "audited")
            .containsEntry("columnName", "title")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testRenameColumnRecordsBothColumnNames() {
        createAuditedTable();

        dataTableService.renameColumn("audited", "title", "headline", ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_COLUMN_RENAMED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("baseName", "audited")
            .containsEntry("fromColumnName", "title")
            .containsEntry("toColumnName", "headline")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testRenameTableRecordsBothTableNames() {
        createAuditedTable();

        dataTableService.renameTable("audited", "audited_renamed", ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_RENAMED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("fromBaseName", "audited")
            .containsEntry("toBaseName", "audited_renamed")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
    }

    @Test
    void testRenameOfAnUnknownTableRecordsAnError() {
        assertThatThrownBy(() -> dataTableService.renameTable("missing", "audited_renamed", ENVIRONMENT_ID))
            .isInstanceOf(BadSqlGrammarException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(DataTableAuditEvents.DATA_TABLE_RENAMED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", BadSqlGrammarException.class.getName())
            .containsEntry("fromBaseName", "missing")
            .containsEntry("toBaseName", "audited_renamed")
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID));
    }

    @Test
    void testDropOfAMissingTableIsNotRecorded() {
        boolean dropped = dataTableService.dropTable("missing", ENVIRONMENT_ID);

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(dropped).isFalse();
        assertThat(page.getContent())
            .extracting(PersistentAuditEvent::getEventType)
            .doesNotContain(DataTableAuditEvents.DATA_TABLE_DELETED);
    }

    private void createAuditedTable() {
        dataTableService.createTable(
            "audited", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);
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
