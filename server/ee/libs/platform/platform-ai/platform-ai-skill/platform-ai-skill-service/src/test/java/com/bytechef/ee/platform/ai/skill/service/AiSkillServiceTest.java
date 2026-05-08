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

package com.bytechef.ee.platform.ai.skill.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.repository.AiSkillRepository;
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
class AiSkillServiceTest {

    @Mock
    private AiSkillRepository aiSkillRepository;

    @InjectMocks
    private AiSkillServiceImpl aiSkillService;

    @Test
    void testCreateAiSkill() {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setName("Test Skill");
        aiSkill.setDescription("Test description");

        when(aiSkillRepository.save(aiSkill)).thenReturn(aiSkill);

        AiSkill result = aiSkillService.createAiSkill(aiSkill);

        assertEquals(aiSkill, result);

        verify(aiSkillRepository).save(aiSkill);
    }

    @Test
    void testDeleteAiSkill() {
        aiSkillService.deleteAiSkill(1L);

        verify(aiSkillRepository).deleteById(1L);
    }

    @Test
    void testExistsByNameReturnsTrue() {
        when(aiSkillRepository.existsByName("Existing Skill")).thenReturn(true);

        assertTrue(aiSkillService.existsByName("Existing Skill"));
    }

    @Test
    void testExistsByNameReturnsFalse() {
        when(aiSkillRepository.existsByName("New Skill")).thenReturn(false);

        assertFalse(aiSkillService.existsByName("New Skill"));
    }

    @Test
    void testGetAiSkill() {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(1L);
        aiSkill.setName("Test Skill");

        when(aiSkillRepository.findById(1L)).thenReturn(Optional.of(aiSkill));

        AiSkill result = aiSkillService.getAiSkill(1L);

        assertEquals(aiSkill, result);
    }

    @Test
    void testGetAiSkillNotFound() {
        when(aiSkillRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiSkillService.getAiSkill(1L));

        assertEquals("AiSkill not found with id: 1", exception.getMessage());
    }

    @Test
    void testGetAiSkills() {
        AiSkill aiSkill1 = new AiSkill();

        aiSkill1.setId(1L);
        aiSkill1.setName("Skill 1");

        AiSkill aiSkill2 = new AiSkill();

        aiSkill2.setId(2L);
        aiSkill2.setName("Skill 2");

        List<AiSkill> aiSkills = List.of(aiSkill1, aiSkill2);

        when(aiSkillRepository.findAll()).thenReturn(aiSkills);

        List<AiSkill> result = aiSkillService.getAiSkills();

        assertEquals(aiSkills, result);
    }

    @Test
    void testUpdateAiSkillPreservesSkillFile() {
        FileEntry originalFileEntry = new FileEntry("skill", "application/zip", "test.skill", "test-url");

        AiSkill existingAiSkill = new AiSkill();

        existingAiSkill.setId(1L);
        existingAiSkill.setName("Original Name");
        existingAiSkill.setDescription("Original Description");
        existingAiSkill.setSkillFile(originalFileEntry);

        when(aiSkillRepository.findById(1L)).thenReturn(Optional.of(existingAiSkill));
        when(aiSkillRepository.save(any(AiSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiSkill result = aiSkillService.updateAiSkill(1L, "Updated Name", "Updated Description");

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(originalFileEntry, result.getSkillFile());

        ArgumentCaptor<AiSkill> captor = ArgumentCaptor.forClass(AiSkill.class);

        verify(aiSkillRepository).save(captor.capture());

        AiSkill savedAiSkill = captor.getValue();

        assertEquals(originalFileEntry, savedAiSkill.getSkillFile());
    }
}
