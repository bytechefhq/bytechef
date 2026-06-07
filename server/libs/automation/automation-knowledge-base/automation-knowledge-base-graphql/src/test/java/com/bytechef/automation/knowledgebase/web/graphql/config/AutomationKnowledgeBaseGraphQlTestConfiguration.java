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

package com.bytechef.automation.knowledgebase.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Test configuration for KnowledgeBase GraphQL integration tests. {@link EnableMethodSecurity} is required so that
 * {@code @PreAuthorize} on controller mutations (e.g. {@code refreshKnowledgeBaseSource}) is enforced in slice tests;
 * without it, anonymous callers slip past admin-only mutations.
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableMethodSecurity
public class AutomationKnowledgeBaseGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer anyScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.anyScalar());
    }

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }

    @Bean
    RuntimeWiringConfigurer mapScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
    }
}
