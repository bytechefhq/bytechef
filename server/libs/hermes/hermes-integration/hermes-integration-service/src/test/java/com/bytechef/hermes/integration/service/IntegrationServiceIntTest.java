
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.integration.service;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
public class IntegrationServiceIntTest {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        for (Integration integration : integrationRepository.findAll()) {
            integrationRepository.deleteById(integration.getId());
        }

        for (Tag tag : tagRepository.findAll()) {
            tagRepository.deleteById(tag.getId());
        }
    }

    @Test
    public void testCreate() {
        Tag tag = tagRepository.save(new Tag("tag1"));

        Integration integration = integrationService.create("name", "description", "category", Set.of("workflow1"),
            Set.of(tag));

        Assertions.assertEquals("category", integration.getCategory());
        Assertions.assertEquals("description", integration.getDescription());
        Assertions.assertEquals("name", integration.getName());
        Assertions.assertEquals(Set.of(tag.getId()), integration.getTagIds());
        Assertions.assertEquals(Set.of("workflow1"), integration.getWorkflowIds());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationService.delete(integration.getId());

        Assertions.assertFalse(
            integrationRepository.findById(integration.getId())
                .isPresent());
    }

    @Test
    public void testGetIntegration() {
        Integration integration = integrationRepository.save(getIntegration());

        Assertions.assertEquals(integration, integrationService.getIntegration(integration.getId()));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrations() {
        integrationRepository.save(getIntegration());

        assertThat(integrationService.getIntegrations()).hasSize(1);
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        Integration updatedIntegration = integrationService.update(integration.getId(), null, null, null, null, null);

        Assertions.assertEquals("category", updatedIntegration.getCategory());
        Assertions.assertEquals("description", updatedIntegration.getDescription());
        Assertions.assertEquals("name", updatedIntegration.getName());
        Assertions.assertTrue(updatedIntegration.getTagIds()
            .isEmpty());
        Assertions.assertEquals(Set.of("workflow1"), updatedIntegration.getWorkflowIds());

        Tag tag = tagRepository.save(new Tag("tag2"));

        updatedIntegration = integrationService.update(integration.getId(), "name2", "description2", "category2",
            Set.of("workflow2"), Set.of(tag));

        Assertions.assertEquals("category2", updatedIntegration.getCategory());
        Assertions.assertEquals("description2", updatedIntegration.getDescription());
        Assertions.assertEquals("name2", updatedIntegration.getName());
        Assertions.assertEquals(Set.of(tag.getId()), updatedIntegration.getTagIds());
        Assertions.assertEquals(Set.of("workflow2"), updatedIntegration.getWorkflowIds());
    }

    private static Integration getIntegration() {
        Integration integration = new Integration();

        integration.addWorkflow("workflow1");

        integration.setCategory("category");
        integration.setDescription("description");
        integration.setName("name");

        return integration;
    }
}
