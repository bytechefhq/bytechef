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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JacksonConfiguration {

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
