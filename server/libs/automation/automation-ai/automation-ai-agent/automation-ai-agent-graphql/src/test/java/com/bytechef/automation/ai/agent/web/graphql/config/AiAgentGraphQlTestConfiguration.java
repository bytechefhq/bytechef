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

package com.bytechef.automation.ai.agent.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Test wiring for the agent GraphQL slice tests. Registers the {@code Map} scalar used by {@code ai-agent.graphqls}
 * (the {@code AiAgentChannel}/{@code AiAgentElement} {@code parameters} fields and their mutation inputs); the
 * {@code @MockitoBean} aggregator on {@link AiAgentGraphQlConfigurationSharedMocks} provides the facade mock
 * separately.
 *
 * @author Ivica Cardic
 */
@Configuration
public class AiAgentGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer mapScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
    }
}
