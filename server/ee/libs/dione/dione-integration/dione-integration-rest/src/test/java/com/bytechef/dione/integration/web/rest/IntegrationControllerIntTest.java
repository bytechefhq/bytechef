
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

package com.bytechef.dione.integration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.dione.integration.dto.IntegrationDTO;
import com.bytechef.dione.integration.facade.IntegrationFacade;
import com.bytechef.category.service.CategoryService;
import com.bytechef.dione.integration.web.rest.mapper.IntegrationMapper;
import com.bytechef.dione.integration.web.rest.model.CreateIntegrationWorkflowRequestModel;
import com.bytechef.dione.integration.web.rest.model.IntegrationModel;
import com.bytechef.dione.integration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
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
            IntegrationDTO integrationDTO = getIntegrationDTO();

            when(integrationFacade.getIntegration(1L)).thenReturn(integrationDTO);

            this.webTestClient
                .get()
                .uri("/integrations/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(IntegrationModel.class)
                .isEqualTo(integrationMapper.convert(integrationDTO));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrationWorkflows() {
        try {
            Workflow workflow = new Workflow("{}", Workflow.Format.JSON, "workflow1", Map.of());

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
    public void testGetIntegrations() {
        IntegrationDTO integrationDTO = getIntegrationDTO();

        when(integrationFacade.searchIntegrations(null, null)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .contains(integrationMapper.convert(integrationDTO))
            .hasSize(1);

        when(integrationFacade.searchIntegrations(List.of(1L), null)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.searchIntegrations(null, List.of(1L))).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.searchIntegrations(List.of(1L), List.of(1L))).thenReturn(List.of(integrationDTO));

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
        IntegrationDTO integrationDTO = getIntegrationDTO();
        IntegrationModel integrationModel = new IntegrationModel()
            .name("name")
            .description("description");

        when(integrationFacade.create(any())).thenReturn(integrationDTO);

        try {
            assert integrationDTO.id() != null;
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
                .isEqualTo(integrationDTO.description())
                .jsonPath("$.id")
                .isEqualTo(integrationDTO.id())
                .jsonPath("$.name")
                .isEqualTo(integrationDTO.name())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<IntegrationDTO> integrationDTOArgumentCaptor = ArgumentCaptor.forClass(IntegrationDTO.class);

        verify(integrationFacade).create(integrationDTOArgumentCaptor.capture());

        IntegrationDTO capturedIntegrationDTO = integrationDTOArgumentCaptor.getValue();

        Assertions.assertEquals(capturedIntegrationDTO.name(), "name");
        Assertions.assertEquals(capturedIntegrationDTO.description(), "description");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegrationWorkflows() throws Exception {
        CreateIntegrationWorkflowRequestModel createIntegrationWorkflowRequestModel = new CreateIntegrationWorkflowRequestModel()
            .label("workflowLabel")
            .description("workflowDescription");
        Workflow workflow = new Workflow(
            "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}", "id",
            Workflow.Format.JSON);

        when(integrationFacade.addWorkflow(anyLong(), any(), any(), any()))
            .thenReturn(workflow);

        try {
            this.webTestClient
                .post()
                .uri("/integrations/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createIntegrationWorkflowRequestModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo("My description")
                .jsonPath("$.id")
                .isEqualTo(workflow.getId())
                .jsonPath("$.label")
                .isEqualTo("New Workflow");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(integrationFacade).addWorkflow(
            anyLong(), nameArgumentCaptor.capture(), descriptionArgumentCaptor.capture(), isNull());

        Assertions.assertEquals("workflowLabel", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutIntegration() {
        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .category(new Category(1L, "category"))
            .description("description")
            .id(1L)
            .name("name2")
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .workflowIds(List.of("workflow1"))
            .build();
        IntegrationModel integrationModel = new IntegrationModel()
            .id(1L)
            .name("name2");

        when(integrationFacade.update(any(IntegrationDTO.class))).thenReturn(integrationDTO);

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
                .isEqualTo(integrationDTO.id())
                .jsonPath("$.description")
                .isEqualTo(integrationDTO.description())
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
                .bodyValue(new UpdateTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
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

    private static IntegrationDTO getIntegrationDTO() {
        return IntegrationDTO.builder()
            .category(new Category(1L, "category"))
            .description("description")
            .id(1L)
            .name("name")
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .workflowIds(List.of("workflow1"))
            .build();
    }

}
