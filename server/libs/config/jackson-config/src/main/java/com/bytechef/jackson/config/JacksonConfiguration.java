/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.jackson.config;

import com.bytechef.commons.util.ConvertHelper;
import com.bytechef.commons.util.JsonHelper;
import com.bytechef.commons.util.XmlHelper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JacksonConfiguration {

    private final JsonComponentModule jsonComponentModule;

    @SuppressFBWarnings("EI")
    public JacksonConfiguration(JsonComponentModule jsonComponentModule) {
        this.jsonComponentModule = jsonComponentModule;
    }

    @Bean
    ConvertHelper convertHelper(ObjectMapper objectMapper) {
        return new ConvertHelper(objectMapper);
    }

    @Bean
    JsonHelper jsonHelper(ObjectMapper objectMapper) {
        return new JsonHelper(objectMapper);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.json());
    }

    @Bean
    XmlMapper xmlMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.xml());
    }

    @Bean
    XmlHelper xmlHelper(XmlMapper xmlMapper) {
        return new XmlHelper(xmlMapper);
    }

    private <T extends ObjectMapper> T buildMapper(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        return objectMapperBuilder
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .featuresToDisable(SerializationFeature.INDENT_OUTPUT)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .modules(new JavaTimeModule(), new Jdk8Module(), new JsonNullableModule(), jsonComponentModule)
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .build();
    }
}
