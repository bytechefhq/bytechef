
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.worker.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.json());
    }

    @Bean
    XmlMapper xmlMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.xml());
    }

    private static <T extends ObjectMapper> T buildMapper(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        return objectMapperBuilder
            .dateFormat(new StdDateFormat().withColonInTimeZone(true))
            .timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .featuresToDisable(SerializationFeature.INDENT_OUTPUT)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .modules(new JavaTimeModule(), new Jdk8Module(), new JsonNullableModule())
            .build();
    }
}
