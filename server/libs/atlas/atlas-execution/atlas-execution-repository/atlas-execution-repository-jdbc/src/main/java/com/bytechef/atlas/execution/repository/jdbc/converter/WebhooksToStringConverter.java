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

package com.bytechef.atlas.execution.repository.jdbc.converter;

import com.bytechef.atlas.execution.domain.Job;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@WritingConverter
public class WebhooksToStringConverter implements Converter<Job.Webhooks, String> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public WebhooksToStringConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convert(Job.Webhooks webhooks) {
        return write(objectMapper, webhooks);
    }

    private String write(ObjectMapper objectMapper, Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
