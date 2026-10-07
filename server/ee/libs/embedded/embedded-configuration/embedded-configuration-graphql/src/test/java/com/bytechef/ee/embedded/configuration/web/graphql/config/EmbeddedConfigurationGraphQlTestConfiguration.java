/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql.config;

import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class EmbeddedConfigurationGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }
}
