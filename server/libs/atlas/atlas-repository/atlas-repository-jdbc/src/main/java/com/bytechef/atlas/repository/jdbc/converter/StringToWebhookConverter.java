
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

package com.bytechef.atlas.repository.jdbc.converter;

import com.bytechef.atlas.domain.Job;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * @author Ivica Cardic
 */
@ReadingConverter
public class StringToWebhookConverter implements Converter<String, Job.Webhook> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public StringToWebhookConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Job.Webhook convert(String source) {
        return source == null ? null : read(objectMapper, source);
    }

    private Job.Webhook read(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, Job.Webhook.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
