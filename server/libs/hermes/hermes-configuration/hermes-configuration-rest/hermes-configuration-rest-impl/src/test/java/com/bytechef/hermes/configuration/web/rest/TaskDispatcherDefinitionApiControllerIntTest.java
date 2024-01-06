/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.hermes.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.service.ActionDefinitionService;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.configuration.web.rest.config.WorkflowConfigurationRestTestConfiguration;
import com.bytechef.hermes.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = WorkflowConfigurationRestTestConfiguration.class)
@WebMvcTest(TaskDispatcherDefinitionApiController.class)
public class TaskDispatcherDefinitionApiControllerIntTest {

    @MockBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @MockBean
    private ActionDefinitionService actionDefinitionService;

    @MockBean
    private ComponentDefinitionService componentDefinitionService;

    @MockBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @MockBean
    TriggerDefinitionFacade triggerDefinitionFacade;

    @MockBean
    private TriggerDefinitionService triggerDefinitionService;

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
    public void testGetTaskDispatcherDefinitions() {
        Mockito.when(taskDispatcherDefinitionService.getTaskDispatcherDefinitions())
            .thenReturn(
                List.of(
                    new TaskDispatcherDefinition("task-dispatcher1"),
                    new TaskDispatcherDefinition("task-dispatcher2")));

        try {
            webTestClient
                .get()
                .uri("/core/task-dispatcher-definitions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(
                    """
                        [
                            {
                                "name":"task-dispatcher1"
                            },
                            {
                                "name":"task-dispatcher2"
                            }
                        ]
                        """);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}
