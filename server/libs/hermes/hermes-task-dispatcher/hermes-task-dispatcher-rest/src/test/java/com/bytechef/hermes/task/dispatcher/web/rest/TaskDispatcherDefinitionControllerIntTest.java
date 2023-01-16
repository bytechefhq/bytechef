
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

package com.bytechef.hermes.task.dispatcher.web.rest;

import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL;
import com.bytechef.hermes.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.hermes.task.dispatcher.web.rest.config.TaskDispatcherDefinitionRestTestConfiguration;
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
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = TaskDispatcherDefinitionRestTestConfiguration.class)
@WebFluxTest(TaskDispatcherDefinitionController.class)
public class TaskDispatcherDefinitionControllerIntTest {

    @MockBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testGetTaskDispatcherDefinitions() {
        Mockito.when(taskDispatcherDefinitionService.getTaskDispatcherDefinitions())
            .thenReturn(
                Flux.fromIterable(List.of(TaskDispatcherDSL.taskDispatcher("task-dispatcher1"),
                    TaskDispatcherDSL.taskDispatcher("task-dispatcher2"))));

        try {
            webTestClient
                .get()
                .uri("/definitions/task-dispatchers")
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
