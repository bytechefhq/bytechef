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

package com.bytechef.commons.data.jdbc.converter;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import org.springframework.core.convert.converter.Converter;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public class MapWrapperToStringConverter implements Converter<MapWrapper, String> {

    private final ObjectMapper objectMapper;

    public MapWrapperToStringConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.rebuild()
            .changeDefaultVisibility(vc -> vc.with(Visibility.ANY))
            .build();
    }

    @Override
    public String convert(MapWrapper source) {
        return write(objectMapper, source.getMap());
    }

    private String write(ObjectMapper objectMapper, Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
