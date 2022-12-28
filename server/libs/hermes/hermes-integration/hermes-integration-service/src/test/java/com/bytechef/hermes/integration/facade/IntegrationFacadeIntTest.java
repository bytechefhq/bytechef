
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

package com.bytechef.hermes.integration.facade;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.repository.IntegrationRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "bytechef.workflow.workflow-repository.jdbc.enabled=true")
public class IntegrationFacadeIntTest {

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private IntegrationRepository integrationRepository;

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
        Integration integration = integrationFacade.create("name", "description", "category", null, List.of("tag1"));

        assertThat(integration.getCategory()).isEqualTo("category");
        assertThat(integration.getDescription()).isEqualTo("description");
        assertThat(integration.getName()).isEqualTo("name");
        assertThat(integration.getId()).isNotNull();
        assertThat(integration.getTagIds()).hasSize(1);
        assertThat(integration.getWorkflowIds()).hasSize(1);

        integration = integrationFacade.create("name", null, null, List.of("workflow2"), null);

        assertThat(integration.getWorkflowIds()).hasSize(1);
        assertThat(integration.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testDelete() {
        Integration integration1 = integrationFacade.create("name", null, null, null, List.of("tag1"));

        Integration integration2 = integrationFacade.create("name", null, null, null, List.of("tag1"));

        Assertions.assertEquals(2, integrationRepository.count());
        Assertions.assertEquals(1, tagRepository.count());

        integrationFacade.delete(integration1.getId());

        Assertions.assertEquals(1, integrationRepository.count());

        integrationFacade.delete(integration2.getId());

        Assertions.assertEquals(1, tagRepository.count());
        Assertions.assertEquals(0, integrationRepository.count());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrationTests() {
        integrationFacade.create("name", null, null, null, List.of("tag1", "tag2"));

        assertThat(integrationFacade.getIntegrationTags()).contains("tag1", "tag2");

        Integration integration = integrationFacade.create("name2", null, null, null, List.of("tag3"));

        assertThat(integrationFacade.getIntegrationTags()).contains("tag1", "tag2", "tag3");

        integrationRepository.deleteById(integration.getId());

        assertThat(integrationFacade.getIntegrationTags()).contains("tag1", "tag2");
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationFacade.create("name", null, null, null, List.of("tag1", "tag2"));

        assertThat(integration.getTagIds()).hasSize(2);
        assertThat(integration.getWorkflowIds()).hasSize(1);

        Integration updatedIntegration = integrationFacade.update(
            integration.getId(), null, null, null, null, List.of("tag1"));

        assertThat(updatedIntegration.getTagIds()).hasSize(1);
    }
}
