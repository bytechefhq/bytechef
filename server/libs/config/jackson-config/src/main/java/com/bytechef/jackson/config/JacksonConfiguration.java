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

package com.bytechef.jackson.config;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.XmlUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.InitializingBean;
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
    @Primary
    public ObjectMapper objectMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.json());
    }

    @Bean
    public XmlMapper xmlMapper() {
        return buildMapper(Jackson2ObjectMapperBuilder.xml());
    }

    private <T extends ObjectMapper> T buildMapper(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        return objectMapperBuilder
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .featuresToDisable(SerializationFeature.INDENT_OUTPUT)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .modules(new JavaTimeModule(), new Jdk8Module(), new JsonNullableModule(), jsonComponentModule)
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .build();
    }

    @Configuration
    static class JsonUtilsConfiguration implements InitializingBean {

        private final ObjectMapper objectMapper;
        private final XmlMapper xmlMapper;

        JsonUtilsConfiguration(ObjectMapper objectMapper, XmlMapper xmlMapper) {
            this.objectMapper = objectMapper;
            this.xmlMapper = xmlMapper;
        }

        @Override
        public void afterPropertiesSet() {
            ConvertUtils.setObjectMapper(objectMapper);
            JsonUtils.setObjectMapper(objectMapper);
            MapUtils.setObjectMapper(objectMapper);
            XmlUtils.setXmlMapper(xmlMapper);
        }
    }
}
