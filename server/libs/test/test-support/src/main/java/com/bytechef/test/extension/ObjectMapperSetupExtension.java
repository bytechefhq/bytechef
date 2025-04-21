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

package com.bytechef.test.extension;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.jackson.JsonComponentModule;

/**
 * This extension is used to setup the ObjectMapper for the tests.
 *
 * @author Ivica Cardic
 */
public class ObjectMapperSetupExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        JacksonConfiguration jacksonConfiguration = new JacksonConfiguration(new JsonComponentModule());

        ObjectMapper objectMapper = jacksonConfiguration.objectMapper();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);
        XmlUtils.setXmlMapper(jacksonConfiguration.xmlMapper());
    }
}
