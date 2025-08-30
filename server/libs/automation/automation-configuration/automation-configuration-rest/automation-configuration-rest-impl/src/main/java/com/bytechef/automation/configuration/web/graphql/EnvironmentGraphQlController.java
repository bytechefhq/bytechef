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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing environments in the automation configuration module.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class EnvironmentGraphQlController {

    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    EnvironmentGraphQlController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * Returns all user environments.
     *
     * @return list of all available environments
     */
    @QueryMapping
    List<Environment> environments() {
        return environmentService.getEnvironments()
            .stream()
            .map(Environment::new)
            .toList();
    }

    record Environment(long id, String name) {

        Environment(com.bytechef.platform.configuration.domain.Environment environment) {
            this(environment.ordinal(), environment.name());
        }
    }
}
