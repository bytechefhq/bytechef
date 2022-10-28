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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.web.rest.ComponentDefinitionController;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Ivica Cardic
 */
@WebFluxTest(ComponentDefinitionController.class)
public class ComponentDefinitionControllerTest {

    private static final List<ComponentDefinitionFactory> COMPONENT_DEFINITION_FACTORIES =
            List.of(() -> ComponentDSL.createComponent("component1"), () -> ComponentDSL.createComponent("component2"));

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testGetComponentDefinitions() {
        try {
            webTestClient
                    .get()
                    .uri("/definitions/components")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .json(
                            """
                                            [
                                                {
                                                    "name":"component1",
                                                    "version":1.0
                                                },
                                                {
                                                    "name":"component2",
                                                    "version":1.0
                                                }
                                            ]
                                            """);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @TestConfiguration
    static class ComponentDefinitionFactoryConfiguration {

        @Bean
        public List<ComponentDefinitionFactory> componentDefinitionFactories() {
            return COMPONENT_DEFINITION_FACTORIES;
        }
    }
}
