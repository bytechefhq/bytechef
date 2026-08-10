/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Test wiring for the AI Gateway GraphQL slice tests. Registers the {@code Long} scalar used across the module's schema
 * files; the {@code @MockitoBean} aggregator on {@link AiGatewayGraphQlConfigurationSharedMocks} provides the
 * service/facade mocks separately.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AiGatewayGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }
}
