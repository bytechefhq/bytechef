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

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestTestConfiguration;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.IntegrationMapper;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationModel;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = EmbeddedConfigurationRestTestConfiguration.class)
@WebMvcTest(value = IntegrationApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
public class IntegrationApiControllerIntTest {

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private IntegrationFacade integrationFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationMapper.IntegrationDTOToIntegrationModelMapper integrationMapper;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testDeleteIntegration() {
        try {
            this.webTestClient
                .delete()
                .uri("/internal/integrations/1")
                .exchange()
                .expectStatus()
                .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);

        verify(integrationFacade).deleteIntegration(argument.capture());

        Assertions.assertEquals(1L, argument.getValue());
    }

    @Test
    public void testGetIntegration() {
        try {
            IntegrationDTO integrationDTO = getIntegrationDTO();

            when(integrationFacade.getIntegration(1L)).thenReturn(integrationDTO);

            this.webTestClient
                .get()
                .uri("/internal/integrations/1")
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
    public void testGetIntegrations() {
        IntegrationDTO integrationDTO = getIntegrationDTO();

        when(integrationFacade.getIntegrations(null, false, null, null, true)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/internal/integrations")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .contains(integrationMapper.convert(integrationDTO))
            .hasSize(1);

        when(integrationFacade.getIntegrations(1L, false, null, null, true)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/internal/integrations?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(null, false, 1L, null, true)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/internal/integrations?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(IntegrationModel.class)
            .hasSize(1);

        when(integrationFacade.getIntegrations(1L, false, 1L, null, true)).thenReturn(List.of(integrationDTO));

        this.webTestClient
            .get()
            .uri("/internal/integrations?categoryIds=1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    public void testPostIntegration() {
        IntegrationDTO integrationDTO = getIntegrationDTO();
        IntegrationModel integrationModel = new IntegrationModel();

        when(integrationFacade.createIntegration(any())).thenReturn(integrationDTO.id());

        try {
            assert integrationDTO.id() != null;

            this.webTestClient
                .post()
                .uri("/internal/integrations")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(integrationModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(integrationDTO.id())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<IntegrationDTO> integrationDTOArgumentCaptor = ArgumentCaptor.forClass(IntegrationDTO.class);

        verify(integrationFacade).createIntegration(integrationDTOArgumentCaptor.capture());

        IntegrationDTO capturedIntegrationDTO = integrationDTOArgumentCaptor.getValue();

        Assertions.assertEquals("componentName", capturedIntegrationDTO.componentName());
    }

    @Test
    public void testPutIntegration() {
        IntegrationModel integrationModel = new IntegrationModel()
            .id(1L);

        try {
            this.webTestClient
                .put()
                .uri("/internal/integrations/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(integrationModel)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    private static IntegrationDTO getIntegrationDTO() {
        return IntegrationDTO.builder()
            .category(new Category(1L, "category"))
            .componentName("componentName")
            .id(1L)
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .integrationWorkflowIds(List.of(1L))
            .name("Name")
            .build();
    }

}
