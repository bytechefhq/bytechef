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

package com.bytechef.automation.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.knowledgebase.config.AutomationKnowledgeBaseIntTestConfiguration;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditEvents;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationKnowledgeBaseIntTestConfiguration.class,
        WorkspaceKnowledgeBaseFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceKnowledgeBaseFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long DEVELOPMENT_ENVIRONMENT_ID = 0;
    private static final long STAGING_ENVIRONMENT_ID = 1;
    private static final long WORKSPACE_ID = 9201;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @Autowired
    private WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @BeforeEach
    void beforeEach() {
        deleteKnowledgeBases();

        jdbcTemplate.update(
            "INSERT INTO workspace " +
                "(id, name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            WORKSPACE_ID, "knowledge-base-audit-workspace");

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        deleteKnowledgeBases();

        jdbcTemplate.update("DELETE FROM workspace WHERE id = ?", WORKSPACE_ID);

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheNewKnowledgeBaseAndWorkspace() {
        KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .containsEntry("name", "Support articles")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(DEVELOPMENT_ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isPresent();
    }

    @Test
    void testCreateWithAnInvalidEnvironmentRecordsAnError() {
        KnowledgeBase knowledgeBase = newKnowledgeBase("Support articles");

        assertThatThrownBy(
            () -> workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(knowledgeBase, WORKSPACE_ID, -1))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", "-1")
            .doesNotContainKeys("knowledgeBaseId", "name");
    }

    @Test
    void testCloneRecordsTheCloneAndItsSource() {
        KnowledgeBase sourceKnowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        KnowledgeBase clonedKnowledgeBase = workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
            sourceKnowledgeBase.getId(), WORKSPACE_ID, STAGING_ENVIRONMENT_ID, "Staging articles");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CLONED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(clonedKnowledgeBase.getId()))
            .containsEntry("name", "Staging articles")
            .containsEntry("sourceKnowledgeBaseId", String.valueOf(sourceKnowledgeBase.getId()))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(STAGING_ENVIRONMENT_ID))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(clonedKnowledgeBase.getId()).isNotEqualTo(sourceKnowledgeBase.getId());
    }

    @Test
    void testCloneOfAKnowledgeBaseOutsideTheWorkspaceRecordsAnError() {
        KnowledgeBase foreignKnowledgeBase = knowledgeBaseRepository.save(newKnowledgeBase("Foreign articles"));

        Long foreignKnowledgeBaseId = foreignKnowledgeBase.getId();

        assertThatThrownBy(() -> workspaceKnowledgeBaseFacade.cloneWorkspaceKnowledgeBase(
            foreignKnowledgeBaseId, WORKSPACE_ID, STAGING_ENVIRONMENT_ID, null))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_CLONED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("sourceKnowledgeBaseId", String.valueOf(foreignKnowledgeBaseId))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("environmentId", String.valueOf(STAGING_ENVIRONMENT_ID))
            .doesNotContainKeys("knowledgeBaseId", "name");
        assertThat(knowledgeBaseRepository.count()).isEqualTo(1);
    }

    @Test
    void testDeleteRecordsTheDeletedKnowledgeBase() {
        KnowledgeBase knowledgeBase = workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            newKnowledgeBase("Support articles"), WORKSPACE_ID, DEVELOPMENT_ENVIRONMENT_ID);

        persistenceAuditEventRepository.deleteAll();

        workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(knowledgeBase.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("knowledgeBaseId", String.valueOf(knowledgeBase.getId()))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(knowledgeBaseRepository.findById(knowledgeBase.getId())).isEmpty();
    }

    @Test
    void testDeleteOfAnUnknownKnowledgeBaseRecordsAnError() {
        assertThatThrownBy(() -> workspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(KnowledgeBaseAuditEvents.KB_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("knowledgeBaseId", String.valueOf(Long.MAX_VALUE));
    }

    private void deleteKnowledgeBases() {
        workspaceKnowledgeBaseRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
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

    private static KnowledgeBase newKnowledgeBase(String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(name);

        return knowledgeBase;
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
