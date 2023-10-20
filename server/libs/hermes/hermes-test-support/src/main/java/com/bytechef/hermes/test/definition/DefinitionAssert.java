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

package com.bytechef.hermes.test.definition;

import com.bytechef.hermes.definition.Definition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.core.io.ClassPathResource;

public class DefinitionAssert {

    private static final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    public static void assertEquals(String taskDescriptorFileName, Definition definition) {
        try {
            JSONAssert.assertEquals(
                    FileUtils.readFileToString(
                            new ClassPathResource(taskDescriptorFileName).getFile(), StandardCharsets.UTF_8),
                    objectMapper.writeValueAsString(definition),
                    true);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
