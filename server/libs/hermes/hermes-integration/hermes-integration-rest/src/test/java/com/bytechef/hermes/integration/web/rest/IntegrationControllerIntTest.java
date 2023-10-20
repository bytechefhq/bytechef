
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.facade.IntegrationFacade;
import com.bytechef.hermes.integration.service.IntegrationService;
import com.bytechef.hermes.integration.web.rest.config.IntegrationRestTestConfiguration;
import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = IntegrationRestTestConfiguration.class)
@WebFluxTest(value = IntegrationController.class)
public class IntegrationControllerIntTest {

    @MockBean
    private IntegrationFacade integrationFacade;

    @MockBean
    private IntegrationService integrationService;

    @MockBean
    private TagService tagService;

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
    public void testGetIntegration() {
        try {
            Integration integration = getIntegration();

            when(integrationService.getIntegration(anyLong())).thenReturn(integration);

            this.webTestClient
                .get()
                .uri("/integrations/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(IntegrationModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetIntegrations() {
        Integration integration = getIntegration();

        when(integrationService.getIntegrations()).thenReturn(List.of(integration));

        try {
            this.webTestClient
                .get()
                .uri("/integrations")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(IntegrationModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegration() {
        Integration integration = getIntegration();
        IntegrationModel integrationModel = new IntegrationModel().name("name")
            .description("description");

        when(integrationFacade.create(anyString(), anyString(), isNull(), isNull(), isNull())).thenReturn(integration);

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
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutIntegration() {
        Integration integration = getIntegration();
        IntegrationModel integrationModel = new IntegrationModel().id(1L)
            .name("name");

        integration.setName("name2");

        when(integrationFacade.update(
            anyLong(), anyString(), isNull(), isNull(), isNull(), isNull())).thenReturn(integration);

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

    private static Integration getIntegration() {
        Integration integration = new Integration();

        integration.addWorkflow("workflow1");

        integration.setId(1L);
        integration.setDescription("name");
        integration.setName("name");

        return integration;
    }
}
