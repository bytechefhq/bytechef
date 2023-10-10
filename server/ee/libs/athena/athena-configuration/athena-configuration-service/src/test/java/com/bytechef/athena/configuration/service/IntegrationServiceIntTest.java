
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.athena.configuration.service;

import com.bytechef.athena.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.athena.configuration.domain.Integration;
import com.bytechef.athena.configuration.repository.IntegrationRepository;
import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class IntegrationServiceIntTest {

    private Category category;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    public void beforeEach() {
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("name"));
    }

    @AfterEach
    public void afterEach() {
        integrationRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Integration integration = integrationRepository.save(getIntegration());

        integration = integrationService.addWorkflow(Validate.notNull(integration.getId(), "id"), "workflow2");

        assertThat(integration.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testCreate() {
        Integration integration = getIntegration();

        Tag tag = tagRepository.save(new Tag("tag1"));

        integration.setTags(List.of(tag));

        integration = integrationService.create(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("categoryId", category.getId())
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));
    }

    @Test
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationService.delete(Validate.notNull(integration.getId(), "id"));

        assertThat(integrationRepository.findById(integration.getId())).isNotPresent();
    }

    @Test
    public void testGetIntegration() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integration).isEqualTo(
            integrationService.getIntegration(Validate.notNull(integration.getId(), "id")));
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integrationService.getIntegrations(null, null)).hasSize(1);

        Category category = new Category("category1");

        category = categoryRepository.save(category);

        integration.setCategory(category);

        integration = integrationRepository.save(integration);

        assertThat(integrationService.getIntegrations(category.getId(), null)).hasSize(1);

        assertThat(integrationService.getIntegrations(Long.MAX_VALUE, null)).hasSize(0);

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        integration.setTags(List.of(tag));

        integrationRepository.save(integration);

        assertThat(integrationService.getIntegrations(null, tag.getId())).hasSize(1);
        assertThat(integrationService.getIntegrations(null, Long.MAX_VALUE)).hasSize(0);
        assertThat(integrationService.getIntegrations(Long.MAX_VALUE, Long.MAX_VALUE)).hasSize(0);
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        Tag tag = tagRepository.save(new Tag("tag2"));

        integration.setDescription("description2");
        integration.setName("name2");
        integration.setTags(List.of(tag));
        integration.setWorkflowIds(List.of("workflow2"));

        Category category2 = categoryRepository.save(new Category("name2"));

        integration.setCategory(category2);
        integration.setDescription("description2");
        integration.setName("name2");
        integration.setTagIds(List.of(Validate.notNull(tag.getId(), "id")));
        integration.setWorkflowIds(List.of("workflow2"));

        integration = integrationService.update(integration);

        assertThat(integration)
            .hasFieldOrPropertyWithValue("categoryId", category2.getId())
            .hasFieldOrPropertyWithValue("description", "description2")
            .hasFieldOrPropertyWithValue("name", "name2")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow2"));
    }

    private Integration getIntegration() {
        return Integration.builder()
            .categoryId(category.getId())
            .description("description")
            .integrationVersion(1)
            .name("name")
            .status(Integration.Status.UNPUBLISHED)
            .workflowIds(List.of("workflow1"))
            .build();
    }
}
