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

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Configuration class for registering the Map scalar type with GraphQL.
 *
 * @author Ivica Cardic
 */
@Configuration
class MapScalarConfiguration {

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(mapScalar());
    }

    private GraphQLScalarType mapScalar() {
        return GraphQLScalarType.newScalar()
            .name("Map")
            .description("A map scalar that represents a JSON object")
            .coercing(new Coercing<Map<String, Object>, Map<String, Object>>() {

                @Override
                public Map<String, Object> serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Map) {
                        return (Map<String, Object>) dataFetcherResult;
                    }

                    throw new CoercingSerializeException("Expected a Map object");
                }

                @Override
                public Map<String, Object> parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof Map) {
                        return (Map<String, Object>) input;
                    }

                    throw new CoercingParseValueException("Expected a Map object");
                }

                @Override
                public Map<String, Object> parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue) {
                        return parseValue(((StringValue) input).getValue());
                    }

                    throw new CoercingParseLiteralException("Expected a StringValue");
                }
            })
            .build();
    }
}
