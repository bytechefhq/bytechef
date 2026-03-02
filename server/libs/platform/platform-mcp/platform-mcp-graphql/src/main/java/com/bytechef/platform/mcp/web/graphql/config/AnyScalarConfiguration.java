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

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Configuration class for registering the Any scalar type with GraphQL.
 *
 * <p>
 * The Any scalar accepts any JSON-compatible value: strings, numbers, booleans, maps, lists, or null.
 *
 * @author Ivica Cardic
 */
@Configuration
class AnyScalarConfiguration {

    @Bean
    RuntimeWiringConfigurer anyScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(anyScalar());
    }

    private GraphQLScalarType anyScalar() {
        return GraphQLScalarType.newScalar()
            .name("Any")
            .description("A scalar that represents any JSON-compatible value")
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
                    return parseLiteralValue((Value<?>) input);
                }

                private Object parseLiteralValue(Value<?> value) {
                    if (value instanceof StringValue stringValue) {
                        return stringValue.getValue();
                    }

                    if (value instanceof IntValue intValue) {
                        return intValue.getValue()
                            .intValue();
                    }

                    if (value instanceof FloatValue floatValue) {
                        return floatValue.getValue()
                            .doubleValue();
                    }

                    if (value instanceof BooleanValue booleanValue) {
                        return booleanValue.isValue();
                    }

                    if (value instanceof NullValue) {
                        return null;
                    }

                    if (value instanceof ObjectValue objectValue) {
                        Map<String, Object> result = new LinkedHashMap<>();

                        for (ObjectField field : objectValue.getObjectFields()) {
                            result.put(field.getName(), parseLiteralValue(field.getValue()));
                        }

                        return result;
                    }

                    if (value instanceof ArrayValue arrayValue) {
                        return arrayValue.getValues()
                            .stream()
                            .map(this::parseLiteralValue)
                            .collect(Collectors.toList());
                    }

                    return value.toString();
                }
            })
            .build();
    }
}
