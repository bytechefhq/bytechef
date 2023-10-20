
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
            
package com.bytechef.athena.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.athena.configuration.dto.IntegrationDTO;
import com.bytechef.athena.configuration.facade.IntegrationFacade;
import com.bytechef.athena.configuration.web.rest.config.IntegrationRestTestConfiguration;
import com.bytechef.athena.configuration.web.rest.mapper.IntegrationMapper;
import com.bytechef.athena.configuration.web.rest.model.IntegrationModel;
import com.bytechef.athena.configuration.web.rest.model.TagModel;
import com.bytechef.athena.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.athena.configuration.web.rest.model.WorkflowRequestModel;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.athena.configuration.web.rest.model.CategoryModel;
import com.bytechef.tag.domain.Tag;

import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

import java.util.Iterator;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = IntegrationRestTestConfiguration.class)
@WebMvcTest(value = IntegrationApiController.class)
public class IntegrationApiControllerIntTest {

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private IntegrationFacade integrationFacade;

    @Autowired
    private IntegrationMapper integrationMapper;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

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
                .isEqualTo(Validate.notNull(integrationMapper.convert(integrationDTO), "integrationModel"));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrationCategories() {
        try {
            when(integrationFacade.getIntegrationCategories()).thenReturn(List.of(new Category(1, "name")));

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
    public void testGetIntegrationWorkflows() {
        try {
            Workflow workflow = new Workflow("workflow1", "{}", Format.JSON, 0);

            when(integrationFacade.getIntegrationWorkflows(1L)).thenReturn(
                List.of(workflow));

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

        when(integrationFacade.getIntegrations(null, null)).thenReturn(List.of(integrationDTO));

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

        when(integrationFacade.getIntegrations(1L, null)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(null, 1L)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(1L, 1L)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/integrations?categoryIds=1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
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
    public void testPostIntegrationWorkflows() {
        String definition = "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}";

        WorkflowRequestModel workflowRequestModel = new WorkflowRequestModel().definition(definition);
        Workflow workflow = new Workflow("id", definition, Format.JSON, 0);

        when(integrationFacade.addWorkflow(anyLong(), any()))
            .thenReturn(workflow);

        try {
            this.webTestClient
                .post()
                .uri("/integrations/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowRequestModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo("My description")
                .jsonPath("$.id")
                .isEqualTo(Validate.notNull(workflow.getId(), "id"))
                .jsonPath("$.label")
                .isEqualTo("New Workflow");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(integrationFacade).addWorkflow(
            anyLong(), isNull());

        Assertions.assertEquals("workflowLabel", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
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
