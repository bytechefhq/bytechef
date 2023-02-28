
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

package com.bytechef.hermes.integration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.facade.IntegrationFacade;
import com.bytechef.category.servicee.CategoryService;
import com.bytechef.hermes.integration.web.rest.mapper.IntegrationMapper;
import com.bytechef.hermes.integration.web.rest.model.CategoryModel;
import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import com.bytechef.hermes.integration.web.rest.model.PostIntegrationWorkflowRequestModel;
import com.bytechef.hermes.integration.web.rest.model.PutIntegrationTagsRequestModel;
import com.bytechef.hermes.integration.web.rest.model.TagModel;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration
@WebFluxTest(value = IntegrationController.class)
public class IntegrationControllerIntTest {

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationMapper integrationMapper;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testDeleteIntegration() {
        try {
            this.webTestClient
                .delete()
                .uri("/integrations/1")
                .exchange()
                .expectStatus()
                .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);

        verify(integrationFacade).delete(argument.capture());

        Assertions.assertEquals(1L, argument.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegration() {
        try {
            Integration integration = getIntegration();

            when(integrationFacade.getIntegration(1L)).thenReturn(integration);

            this.webTestClient
                .get()
                .uri("/integrations/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(IntegrationModel.class)
                .isEqualTo(integrationMapper.convert(integration));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrationWorkflows() {
        try {
            Workflow workflow = new Workflow("workflow1", "{}", Workflow.Format.JSON, Map.of());

            when(integrationFacade.getIntegrationWorkflows(1L)).thenReturn(List.of(workflow));

            this.webTestClient
                .get()
                .uri("/integrations/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrationCategories() {
        try {
            when(categoryService.getCategories()).thenReturn(List.of(new Category(1, "name")));

            this.webTestClient
                .get()
                .uri("/integrations/categories")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CategoryModel.class)
                .hasSize(1);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrationTags() {
        when(integrationFacade.getIntegrationTags()).thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/integrations/tags")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo(1)
                .jsonPath("$.[1].id")
                .isEqualTo(2)
                .jsonPath("$.[0].name")
                .isEqualTo("tag1")
                .jsonPath("$.[1].name")
                .isEqualTo("tag2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = getIntegration();

        when(integrationFacade.getIntegrations(null, null)).thenReturn(List.of(integration));

        this.webTestClient
            .get()
            .uri("/integrations")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .contains(integrationMapper.convert(integration))
            .hasSize(1);

        when(integrationFacade.getIntegrations(List.of(1L), null)).thenReturn(List.of(integration));

        this.webTestClient
            .get()
            .uri("/integrations?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(null, List.of(1L))).thenReturn(List.of(integration));

        this.webTestClient
            .get()
            .uri("/integrations?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(List.of(1L), List.of(1L))).thenReturn(List.of(integration));

        this.webTestClient
            .get()
            .uri("/integrations?categoryIds=1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegration() {
        Integration integration = getIntegration();
        IntegrationModel integrationModel = new IntegrationModel()
            .name("name")
            .description("description");

        when(integrationFacade.create(any())).thenReturn(integration);

        try {
            assert integration.getId() != null;
            this.webTestClient
                .post()
                .uri("/integrations")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(integrationModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo(integration.getDescription())
                .jsonPath("$.id")
                .isEqualTo(integration.getId())
                .jsonPath("$.name")
                .isEqualTo(integration.getName())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Integration> integrationArgumentCaptor = ArgumentCaptor.forClass(Integration.class);

        verify(integrationFacade).create(integrationArgumentCaptor.capture());

        Integration capturedIntegration = integrationArgumentCaptor.getValue();

        Assertions.assertEquals(capturedIntegration.getName(), "name");
        Assertions.assertEquals(capturedIntegration.getDescription(), "description");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegrationWorkflows() {
        Integration integration = getIntegration();
        PostIntegrationWorkflowRequestModel postIntegrationWorkflowRequestModel = new PostIntegrationWorkflowRequestModel()
            .name("workflowName")
            .description("workflowDescription");

        when(integrationFacade.addWorkflow(1L, "workflowName", "workflowDescription", null))
            .thenReturn(integration);

        try {
            assert integration.getId() != null;
            this.webTestClient
                .post()
                .uri("/integrations/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(postIntegrationWorkflowRequestModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo(integration.getDescription())
                .jsonPath("$.id")
                .isEqualTo(integration.getId())
                .jsonPath("$.name")
                .isEqualTo(integration.getName())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(integrationFacade).addWorkflow(anyLong(), nameArgumentCaptor.capture(),
            descriptionArgumentCaptor.capture(), isNull());

        Assertions.assertEquals("workflowName", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutIntegration() {
        Integration integration = getIntegration();
        IntegrationModel integrationModel = new IntegrationModel()
            .id(1L)
            .name("name2");

        integration.setName("name2");

        when(integrationFacade.update(integration)).thenReturn(integration);

        try {
            this.webTestClient
                .put()
                .uri("/integrations/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(integrationModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(integration.getId())
                .jsonPath("$.description")
                .isEqualTo(integration.getDescription())
                .jsonPath("$.name")
                .isEqualTo("name2")
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NP")
    public void testPutIntegrationTags() {
        try {
            this.webTestClient
                .put()
                .uri("/integrations/1/tags")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PutIntegrationTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(integrationFacade).update(anyLong(), tagsArgumentCaptor.capture());

        List<Tag> capturedTags = tagsArgumentCaptor.getValue();

        Iterator<Tag> tagIterator = capturedTags.iterator();

        Tag capturedTag = tagIterator.next();

        Assertions.assertEquals("tag1", capturedTag.getName());
    }

    @ComponentScan(basePackages = {
        "com.bytechef.hermes.integration.web.rest",
        "com.bytechef.atlas.web.rest.mapper"
    })
    @SpringBootConfiguration
    public static class IntegrationRestTestConfiguration {
    }

    private static Integration getIntegration() {
        Integration integration = new Integration();

        integration.addWorkflow("workflow1");

        integration.setCategory(new Category(1L, "category"));
        integration.setDescription("description");
        integration.setId(1L);
        integration.setName("name");
        integration.setTags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        return integration;
    }
}
