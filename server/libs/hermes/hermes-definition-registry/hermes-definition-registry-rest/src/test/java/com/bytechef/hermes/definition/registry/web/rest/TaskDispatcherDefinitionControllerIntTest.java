
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.config.RegistryDefinitionRestTestConfiguration;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = RegistryDefinitionRestTestConfiguration.class)
@WebFluxTest(TaskDispatcherDefinitionController.class)
public class TaskDispatcherDefinitionControllerIntTest {

    @MockBean
    private ActionDefinitionService actionDefinitionService;

    @MockBean
    private ComponentDefinitionFacade componentDefinitionFacade;

    @MockBean
    private ComponentDefinitionService componentDefinitionService;

    @MockBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testGetTaskDispatcherDefinitions() {
        Mockito.when(taskDispatcherDefinitionService.getTaskDispatcherDefinitionsMono())
            .thenReturn(
                Mono.just(
                    List.of(
                        TaskDispatcherDSL.taskDispatcher("task-dispatcher1"),
                        TaskDispatcherDSL.taskDispatcher("task-dispatcher2"))));

        try {
            webTestClient
                .get()
                .uri("/task-dispatcher-definitions")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(
                    """
                        [
                            {
                                "name":"task-dispatcher1",
                                "version":1.0
                            },
                            {
                                "name":"task-dispatcher2",
                                "version":1.0
                            }
                        ]
                        """);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}
