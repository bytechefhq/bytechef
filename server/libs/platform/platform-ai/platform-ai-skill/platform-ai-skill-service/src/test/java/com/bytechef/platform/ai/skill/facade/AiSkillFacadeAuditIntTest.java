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

package com.bytechef.platform.ai.skill.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.ai.skill.audit.AiSkillAuditEvents;
import com.bytechef.platform.ai.skill.config.AiSkillIntTestConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AiSkillIntTestConfiguration.class, AiSkillFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
class AiSkillFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String CONFIDENTIAL_INSTRUCTIONS = "Confidential instructions";
    private static final long UNKNOWN_AI_SKILL_ID = Long.MAX_VALUE;

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        aiSkillRepository.deleteAll();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateFromInstructionsRecordsTheSkillWithoutItsInstructions() {
        AiSkill aiSkill = aiSkillFacade.createAiSkillFromInstructions(
            "audited-skill", "Audited description", CONFIDENTIAL_INSTRUCTIONS);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiSkillAuditEvents.AI_SKILL_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()))
            .containsEntry("name", "audited-skill")
            .doesNotContainValue(CONFIDENTIAL_INSTRUCTIONS);
        assertThat(findEventTypes()).containsExactly(AiSkillAuditEvents.AI_SKILL_CREATED);
    }

    @Test
    void testUpdateRecordsTheNewName() {
        AiSkill aiSkill = createAiSkill();

        persistenceAuditEventRepository.deleteAll();

        aiSkillFacade.updateAiSkill(aiSkill.getId(), "renamed-skill", null);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiSkillAuditEvents.AI_SKILL_UPDATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()))
            .containsEntry("name", "renamed-skill");
    }

    @Test
    void testUpdateOfAnUnknownSkillRecordsAnError() {
        assertThatThrownBy(() -> aiSkillFacade.updateAiSkill(UNKNOWN_AI_SKILL_ID, "renamed-skill", null))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiSkillAuditEvents.AI_SKILL_UPDATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("aiSkillId", String.valueOf(UNKNOWN_AI_SKILL_ID))
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .doesNotContainKey("name");
    }

    @Test
    void testFileChangesRecordThePathsWithoutTheContent() {
        AiSkill aiSkill = createAiSkill();

        persistenceAuditEventRepository.deleteAll();

        aiSkillFacade.createAdditionalFilesInSkill(
            aiSkill.getId(), Map.of("references/a.md", "confidential a", "references/b.md", "confidential b"));

        assertThat(findSingleEvent(AiSkillAuditEvents.AI_SKILL_FILES_ADDED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()))
            .doesNotContainKey("dataError")
            .doesNotContainValue("confidential a");

        aiSkillFacade.updateAiSkillContent(aiSkill.getId(), "references/a.md", "confidential update");

        assertThat(findSingleEvent(AiSkillAuditEvents.AI_SKILL_CONTENT_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()))
            .containsEntry("path", "references/a.md")
            .doesNotContainValue("confidential update");

        aiSkillFacade.removeFileInSkill(aiSkill.getId(), "references/b.md");

        assertThat(findSingleEvent(AiSkillAuditEvents.AI_SKILL_FILE_REMOVED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()))
            .containsEntry("path", "references/b.md");
    }

    @Test
    void testDeleteRecordsTheSkill() {
        AiSkill aiSkill = createAiSkill();

        persistenceAuditEventRepository.deleteAll();

        aiSkillFacade.deleteAiSkill(aiSkill.getId());

        assertThat(findSingleEvent(AiSkillAuditEvents.AI_SKILL_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiSkillId", String.valueOf(aiSkill.getId()));
    }

    private AiSkill createAiSkill() {
        return aiSkillFacade.createAiSkillFromInstructions("audited-skill", null, CONFIDENTIAL_INSTRUCTIONS);
    }

    private List<String> findEventTypes() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        return persistentAuditEvents.stream()
            .map(PersistentAuditEvent::getEventType)
            .toList();
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
