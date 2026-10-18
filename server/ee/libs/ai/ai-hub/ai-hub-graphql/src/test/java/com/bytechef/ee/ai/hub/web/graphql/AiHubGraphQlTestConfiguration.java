/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Supplies the scalars ({@code Long}, {@code Any}, {@code Map}) that the platform schemas declare and the running
 * application wires elsewhere.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
class AiHubGraphQlTestConfiguration {

    static GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
            .name("Long")
            .coercing(new Coercing<Long, Long>() {

                @Override
                public Long serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Number number) {
                        return number.longValue();
                    }

                    throw new CoercingSerializeException("Expected a number but was: " + dataFetcherResult);
                }

                @Override
                public Long parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof Number number) {
                        return number.longValue();
                    }

                    throw new CoercingParseValueException("Expected a number but was: " + input);
                }

                @Override
                public Long parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof IntValue intValue) {
                        return intValue.getValue()
                            .longValue();
                    }

                    if (input instanceof StringValue stringValue) {
                        return Long.parseLong(stringValue.getValue());
                    }

                    throw new CoercingParseLiteralException("Expected an int literal but was: " + input);
                }
            })
            .build();
    }

    static GraphQLScalarType passThroughScalar(String name) {
        return GraphQLScalarType.newScalar()
            .name(name)
            .coercing(new Coercing<Object, Object>() {

                @Override
                public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    return dataFetcherResult;
                }

                @Override
                public Object parseValue(Object input) throws CoercingParseValueException {
                    return input;
                }

                @Override
                public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                    return input;
                }
            })
            .build();
    }

    @Bean
    RuntimeWiringConfigurer scalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(longScalar())
            .scalar(passThroughScalar("Any"))
            .scalar(passThroughScalar("Map"));
    }
}
