/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql.config;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.math.BigInteger;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AuditGraphQlTestConfiguration {

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(longScalar());
    }

    private GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
            .name("Long")
            .description("A Long scalar that represents a 64-bit signed integer")
            .coercing(new Coercing<Long, Long>() {

                @Override
                public Long serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Long) {
                        return (Long) dataFetcherResult;
                    }

                    if (dataFetcherResult instanceof Number) {
                        return ((Number) dataFetcherResult).longValue();
                    }

                    if (dataFetcherResult instanceof Instant) {
                        return ((Instant) dataFetcherResult).toEpochMilli();
                    }

                    if (dataFetcherResult instanceof String) {
                        try {
                            return Long.parseLong((String) dataFetcherResult);
                        } catch (NumberFormatException numberFormatException) {
                            throw new CoercingSerializeException(
                                "Expected a Long value but was: " + dataFetcherResult, numberFormatException);
                        }
                    }

                    throw new CoercingSerializeException("Expected a Long object but was: " + dataFetcherResult);
                }

                @Override
                public Long parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof Long) {
                        return (Long) input;
                    }

                    if (input instanceof Number) {
                        return ((Number) input).longValue();
                    }

                    if (input instanceof String) {
                        try {
                            return Long.parseLong((String) input);
                        } catch (NumberFormatException numberFormatException) {
                            throw new CoercingParseValueException(
                                "Expected a Long value but was: " + input, numberFormatException);
                        }
                    }

                    throw new CoercingParseValueException("Expected a Long object but was: " + input);
                }

                @Override
                public Long parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof IntValue) {
                        BigInteger bigInteger = ((IntValue) input).getValue();

                        return bigInteger.longValue();
                    }

                    if (input instanceof StringValue) {
                        try {
                            return Long.parseLong(((StringValue) input).getValue());
                        } catch (NumberFormatException numberFormatException) {
                            throw new CoercingParseLiteralException(
                                "Expected a Long value but was: " + input, numberFormatException);
                        }
                    }

                    throw new CoercingParseLiteralException(
                        "Expected an IntValue or StringValue but was: " + input);
                }
            })
            .build();
    }
}
