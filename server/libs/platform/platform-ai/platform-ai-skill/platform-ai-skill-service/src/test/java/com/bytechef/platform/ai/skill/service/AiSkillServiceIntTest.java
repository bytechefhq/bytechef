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

package com.bytechef.platform.ai.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.config.AiSkillIntTestConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiSkillIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiSkillServiceIntTest {

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @Autowired
    private AiSkillService aiSkillService;

    @AfterEach
    void afterEach() {
        aiSkillRepository.deleteAll();
    }

    @Test
    void testCreateAiSkill() {
        AiSkill aiSkill = newAiSkill("Test Skill", "Test description");

        AiSkill createdAiSkill = aiSkillService.createAiSkill(aiSkill);

        assertThat(createdAiSkill.getId()).isNotNull();

        AiSkill persistedAiSkill = aiSkillRepository.findById(createdAiSkill.getId())
            .orElseThrow();

        assertThat(persistedAiSkill.getName()).isEqualTo("Test Skill");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Test description");
    }

    @Test
    void testDeleteAiSkill() {
        AiSkill aiSkill = aiSkillRepository.save(newAiSkill("Skill To Delete", null));

        aiSkillService.deleteAiSkill(aiSkill.getId());

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isEmpty();
    }

    @Test
    void testExistsByNameReturnsTrue() {
        aiSkillRepository.save(newAiSkill("Existing Skill", null));

        assertThat(aiSkillService.existsByName("Existing Skill")).isTrue();
    }

    @Test
    void testExistsByNameReturnsFalse() {
        aiSkillRepository.save(newAiSkill("Existing Skill", null));

        assertThat(aiSkillService.existsByName("New Skill")).isFalse();
    }

    @Test
    void testGetAiSkill() {
        AiSkill aiSkill = aiSkillRepository.save(newAiSkill("Test Skill", null));

        AiSkill result = aiSkillService.getAiSkill(aiSkill.getId());

        assertThat(result).isEqualTo(aiSkill);
        assertThat(result.getName()).isEqualTo("Test Skill");
    }

    @Test
    void testGetAiSkillNotFound() {
        AiSkill aiSkill = aiSkillRepository.save(newAiSkill("Deleted Skill", null));

        long missingId = aiSkill.getId();

        aiSkillRepository.deleteById(missingId);

        assertThatThrownBy(() -> aiSkillService.getAiSkill(missingId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("AiSkill not found with id: " + missingId);
    }

    @Test
    void testGetAiSkills() {
        AiSkill aiSkill1 = aiSkillRepository.save(newAiSkill("Skill 1", null));
        AiSkill aiSkill2 = aiSkillRepository.save(newAiSkill("Skill 2", null));

        List<AiSkill> result = aiSkillService.getAiSkills();

        assertThat(result).containsExactlyInAnyOrder(aiSkill1, aiSkill2);
    }

    @Test
    void testUpdateAiSkillPreservesSkillFile() {
        FileEntry originalFileEntry = new FileEntry("skill", "application/zip", "test.skill", "test-url");

        AiSkill existingAiSkill = newAiSkill("Original Name", "Original Description");

        existingAiSkill.setSkillFile(originalFileEntry);

        existingAiSkill = aiSkillRepository.save(existingAiSkill);

        AiSkill result = aiSkillService.updateAiSkill(existingAiSkill.getId(), "Updated Name", "Updated Description");

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getSkillFile()).isEqualTo(originalFileEntry);

        AiSkill persistedAiSkill = aiSkillRepository.findById(existingAiSkill.getId())
            .orElseThrow();

        assertThat(persistedAiSkill.getName()).isEqualTo("Updated Name");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Updated Description");
        assertThat(persistedAiSkill.getSkillFile()).isEqualTo(originalFileEntry);
    }

    private static AiSkill newAiSkill(String name, String description) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setName(name);
        aiSkill.setDescription(description);
        aiSkill.setSkillFile(new FileEntry("skill", "application/zip", name + ".skill", "url-" + name));

        return aiSkill;
    }
}
