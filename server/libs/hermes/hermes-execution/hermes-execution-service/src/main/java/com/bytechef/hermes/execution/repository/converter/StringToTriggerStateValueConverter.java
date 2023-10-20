
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

package com.bytechef.hermes.execution.repository.converter;

import com.bytechef.hermes.execution.domain.TriggerState.TriggerStateValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * @author Ivica Cardic
 */
@ReadingConverter
public class StringToTriggerStateValueConverter implements Converter<String, TriggerStateValue> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public StringToTriggerStateValueConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TriggerStateValue convert(String source) {
        return source == null ? null : read(objectMapper, source);
    }

    private TriggerStateValue read(ObjectMapper objectMapper, String json) {
        try {
            TriggerStateValue triggerStateValue = objectMapper.readValue(json, TriggerStateValue.class);

            return new TriggerStateValue(
                objectMapper.convertValue(triggerStateValue.value(), Class.forName(triggerStateValue.classname())),
                triggerStateValue.classname());
        } catch (JsonProcessingException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
