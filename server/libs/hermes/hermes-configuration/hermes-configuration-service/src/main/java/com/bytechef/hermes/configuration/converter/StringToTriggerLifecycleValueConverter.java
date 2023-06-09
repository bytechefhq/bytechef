
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

package com.bytechef.hermes.configuration.converter;

import com.bytechef.hermes.configuration.domain.TriggerLifecycle.TriggerLifecycleValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * @author Ivica Cardic
 */
@ReadingConverter
public class StringToTriggerLifecycleValueConverter implements Converter<String, TriggerLifecycleValue> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public StringToTriggerLifecycleValueConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TriggerLifecycleValue convert(String source) {
        return source == null ? null : read(objectMapper, source);
    }

    private TriggerLifecycleValue read(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, TriggerLifecycleValue.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
