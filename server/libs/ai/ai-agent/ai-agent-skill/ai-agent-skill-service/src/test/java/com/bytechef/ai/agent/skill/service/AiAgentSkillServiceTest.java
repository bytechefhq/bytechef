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

package com.bytechef.ai.agent.skill.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.repository.AiAgentSkillRepository;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiAgentSkillServiceTest {

    @Mock
    private AiAgentSkillRepository aiAgentSkillRepository;

    @InjectMocks
    private AiAgentSkillServiceImpl aiAgentSkillService;

    @Test
    void testCreateAiAgentSkill() {
        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setName("Test Skill");
        aiAgentSkill.setDescription("Test description");

        when(aiAgentSkillRepository.save(aiAgentSkill)).thenReturn(aiAgentSkill);

        AiAgentSkill result = aiAgentSkillService.createAiAgentSkill(aiAgentSkill);

        assertEquals(aiAgentSkill, result);

        verify(aiAgentSkillRepository).save(aiAgentSkill);
    }

    @Test
    void testDeleteAiAgentSkill() {
        aiAgentSkillService.deleteAiAgentSkill(1L);

        verify(aiAgentSkillRepository).deleteById(1L);
    }

    @Test
    void testExistsByNameReturnsTrue() {
        when(aiAgentSkillRepository.existsByName("Existing Skill")).thenReturn(true);

        assertTrue(aiAgentSkillService.existsByName("Existing Skill"));
    }

    @Test
    void testExistsByNameReturnsFalse() {
        when(aiAgentSkillRepository.existsByName("New Skill")).thenReturn(false);

        assertFalse(aiAgentSkillService.existsByName("New Skill"));
    }

    @Test
    void testGetAiAgentSkill() {
        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setId(1L);
        aiAgentSkill.setName("Test Skill");

        when(aiAgentSkillRepository.findById(1L)).thenReturn(Optional.of(aiAgentSkill));

        AiAgentSkill result = aiAgentSkillService.getAiAgentSkill(1L);

        assertEquals(aiAgentSkill, result);
    }

    @Test
    void testGetAiAgentSkillNotFound() {
        when(aiAgentSkillRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiAgentSkillService.getAiAgentSkill(1L));

        assertEquals("AiAgentSkill not found with id: 1", exception.getMessage());
    }

    @Test
    void testGetAiAgentSkills() {
        AiAgentSkill aiAgentSkill1 = new AiAgentSkill();

        aiAgentSkill1.setId(1L);
        aiAgentSkill1.setName("Skill 1");

        AiAgentSkill aiAgentSkill2 = new AiAgentSkill();

        aiAgentSkill2.setId(2L);
        aiAgentSkill2.setName("Skill 2");

        List<AiAgentSkill> aiAgentSkills = List.of(aiAgentSkill1, aiAgentSkill2);

        when(aiAgentSkillRepository.findAll()).thenReturn(aiAgentSkills);

        List<AiAgentSkill> result = aiAgentSkillService.getAiAgentSkills();

        assertEquals(aiAgentSkills, result);
    }

    @Test
    void testUpdateAiAgentSkillPreservesSkillFile() {
        FileEntry originalFileEntry = new FileEntry("skill", "application/zip", "test.skill", "test-url");

        AiAgentSkill existingAiAgentSkill = new AiAgentSkill();

        existingAiAgentSkill.setId(1L);
        existingAiAgentSkill.setName("Original Name");
        existingAiAgentSkill.setDescription("Original Description");
        existingAiAgentSkill.setSkillFile(originalFileEntry);

        when(aiAgentSkillRepository.findById(1L)).thenReturn(Optional.of(existingAiAgentSkill));
        when(aiAgentSkillRepository.save(any(AiAgentSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAgentSkill result = aiAgentSkillService.updateAiAgentSkill(1L, "Updated Name", "Updated Description");

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(originalFileEntry, result.getSkillFile());

        ArgumentCaptor<AiAgentSkill> captor = ArgumentCaptor.forClass(AiAgentSkill.class);

        verify(aiAgentSkillRepository).save(captor.capture());

        AiAgentSkill savedAiAgentSkill = captor.getValue();

        assertEquals(originalFileEntry, savedAiAgentSkill.getSkillFile());
    }
}
