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

package com.bytechef.automation.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestTestConfiguration;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
@ContextConfiguration(classes = AutomationConfigurationRestTestConfiguration.class)
@WebMvcTest(value = ConnectionTagApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
@MockitoBean(types = {
    CategoryService.class, ProjectCategoryFacade.class, ProjectDeploymentFacade.class, ProjectFacade.class,
    ProjectService.class, ProjectTagFacade.class, ProjectWorkflowFacade.class, WorkflowService.class,
    WorkspaceFacade.class
})
class ConnectionTagApiControllerIntTest {

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testGetConnectionTags() {
        when(connectionFacade.getConnectionTags(PlatformType.AUTOMATION))
            .thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        webTestClient
            .get()
            .uri("/internal/connections/tags")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(2)
            .jsonPath("$[0].id")
            .isEqualTo(1)
            .jsonPath("$[0].name")
            .isEqualTo("tag1")
            .jsonPath("$[1].id")
            .isEqualTo(2)
            .jsonPath("$[1].name")
            .isEqualTo("tag2");

        verify(connectionFacade).getConnectionTags(PlatformType.AUTOMATION);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateConnectionTags() {
        webTestClient
            .put()
            .uri("/internal/connections/5/tags")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                new UpdateTagsRequestModel().tags(
                    List.of(new TagModel().id(3L)
                        .name("tag1"), new TagModel().name("tag2"))))
            .exchange()
            .expectStatus()
            .isNoContent();

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionFacade).update(eq(5L), tagsArgumentCaptor.capture());

        assertThat(tagsArgumentCaptor.getValue())
            .extracting(Tag::getId, Tag::getName)
            .containsExactly(tuple(3L, "tag1"), tuple(null, "tag2"));
    }
}
