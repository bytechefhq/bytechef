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

package com.bytechef.commons.data.jdbc.converter;

import com.bytechef.commons.data.jdbc.wrapper.MapListWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class StringToMapListWrapperConverter implements Converter<String, MapListWrapper> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public StringToMapListWrapperConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MapListWrapper convert(String source) {
        return source == null ? null : new MapListWrapper(read(objectMapper, source));
    }

    private List read(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
