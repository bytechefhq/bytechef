
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

package com.bytechef.helios.project.web.rest;

import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.helios.project.facade.ProjectInstanceFacade;
import com.bytechef.helios.project.facade.WorkflowExecutionFacade;
import com.bytechef.helios.project.web.rest.config.ProjectRestTestConfiguration;
import com.bytechef.tag.domain.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = ProjectRestTestConfiguration.class)
@WebMvcTest(value = ProjectTagController.class)
public class ProjectTagControllerIntTest {

    @MockBean
    private ProjectInstanceFacade projectInstanceFacade;

    @MockBean
    private ProjectFacade projectFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @MockBean
    private WorkflowExecutionFacade workflowExecutionFacade;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testGetProjectTags() {
        when(projectFacade.getProjectTags()).thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/automation/project-tags")
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
}
