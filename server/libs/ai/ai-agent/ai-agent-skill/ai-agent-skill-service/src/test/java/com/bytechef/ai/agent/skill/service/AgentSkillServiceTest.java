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

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import com.bytechef.ai.agent.skill.repository.AgentSkillRepository;
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
class AgentSkillServiceTest {

    @Mock
    private AgentSkillRepository agentSkillRepository;

    @InjectMocks
    private AgentSkillServiceImpl agentSkillService;

    @Test
    void testCreateAgentSkill() {
        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setName("Test Skill");
        agentSkill.setDescription("Test description");

        when(agentSkillRepository.save(agentSkill)).thenReturn(agentSkill);

        AgentSkill result = agentSkillService.createAgentSkill(agentSkill);

        assertEquals(agentSkill, result);

        verify(agentSkillRepository).save(agentSkill);
    }

    @Test
    void testDeleteAgentSkill() {
        agentSkillService.deleteAgentSkill(1L);

        verify(agentSkillRepository).deleteById(1L);
    }

    @Test
    void testExistsByNameReturnsTrue() {
        when(agentSkillRepository.existsByName("Existing Skill")).thenReturn(true);

        assertTrue(agentSkillService.existsByName("Existing Skill"));
    }

    @Test
    void testExistsByNameReturnsFalse() {
        when(agentSkillRepository.existsByName("New Skill")).thenReturn(false);

        assertFalse(agentSkillService.existsByName("New Skill"));
    }

    @Test
    void testGetAgentSkill() {
        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setId(1L);
        agentSkill.setName("Test Skill");

        when(agentSkillRepository.findById(1L)).thenReturn(Optional.of(agentSkill));

        AgentSkill result = agentSkillService.getAgentSkill(1L);

        assertEquals(agentSkill, result);
    }

    @Test
    void testGetAgentSkillNotFound() {
        when(agentSkillRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> agentSkillService.getAgentSkill(1L));

        assertEquals("AgentSkill not found with id: 1", exception.getMessage());
    }

    @Test
    void testGetAgentSkills() {
        AgentSkill agentSkill1 = new AgentSkill();

        agentSkill1.setId(1L);
        agentSkill1.setName("Skill 1");

        AgentSkill agentSkill2 = new AgentSkill();

        agentSkill2.setId(2L);
        agentSkill2.setName("Skill 2");

        List<AgentSkill> agentSkills = List.of(agentSkill1, agentSkill2);

        when(agentSkillRepository.findAll()).thenReturn(agentSkills);

        List<AgentSkill> result = agentSkillService.getAgentSkills();

        assertEquals(agentSkills, result);
    }

    @Test
    void testUpdateAgentSkillPreservesSkillFile() {
        FileEntry originalFileEntry = new FileEntry("skill", "application/zip", "test.skill", "test-url");

        AgentSkill existingAgentSkill = new AgentSkill();

        existingAgentSkill.setId(1L);
        existingAgentSkill.setName("Original Name");
        existingAgentSkill.setDescription("Original Description");
        existingAgentSkill.setSkillFileEntry(originalFileEntry);

        when(agentSkillRepository.findById(1L)).thenReturn(Optional.of(existingAgentSkill));
        when(agentSkillRepository.save(any(AgentSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentSkill result = agentSkillService.updateAgentSkill(1L, "Updated Name", "Updated Description");

        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(originalFileEntry, result.getSkillFileEntry());

        ArgumentCaptor<AgentSkill> captor = ArgumentCaptor.forClass(AgentSkill.class);

        verify(agentSkillRepository).save(captor.capture());

        AgentSkill savedAgentSkill = captor.getValue();

        assertEquals(originalFileEntry, savedAgentSkill.getSkillFileEntry());
    }
}
