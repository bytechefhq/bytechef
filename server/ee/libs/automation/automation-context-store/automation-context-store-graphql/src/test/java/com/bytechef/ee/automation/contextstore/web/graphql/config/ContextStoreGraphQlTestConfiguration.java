/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Test wiring for the Context Store GraphQL slice tests. Registers the {@code Long} and {@code Map} scalars used by
 * {@code context-store.graphqls}; the {@code @MockitoBean} aggregator on
 * {@link ContextStoreGraphQlConfigurationSharedMocks} provides the service/facade mocks separately.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Configuration
@EnableMethodSecurity
public class ContextStoreGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }

    @Bean
    RuntimeWiringConfigurer mapScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
    }
}
