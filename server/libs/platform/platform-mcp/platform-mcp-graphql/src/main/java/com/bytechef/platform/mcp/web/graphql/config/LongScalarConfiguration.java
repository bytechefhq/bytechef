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

package com.bytechef.platform.mcp.web.graphql.config;

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
 * Configuration class for registering the Long scalar type with GraphQL.
 *
 * @author Ivica Cardic
 */
@Configuration
class LongScalarConfiguration {

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
                        } catch (NumberFormatException e) {
                            throw new CoercingSerializeException(
                                "Expected a Long value but was: " + dataFetcherResult, e);
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
                        } catch (NumberFormatException e) {
                            throw new CoercingParseValueException("Expected a Long value but was: " + input, e);
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
                        } catch (NumberFormatException e) {
                            throw new CoercingParseLiteralException("Expected a Long value but was: " + input, e);
                        }
                    }

                    throw new CoercingParseLiteralException("Expected an IntValue or StringValue but was: " + input);
                }
            })
            .build();
    }
}
