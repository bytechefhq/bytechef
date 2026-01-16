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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.FieldOptionModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerFormInputModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerFormModel;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = PlatformConfigurationMapperSpringConfig.class)
public interface TriggerFormMapper extends Converter<Map<String, Object>, TriggerFormModel> {

    @Override
    TriggerFormModel convert(Map<String, Object> triggerForm);

    FieldOptionModel convertToFieldOptionModel(Map<String, Object> fieldOption);

    TriggerFormInputModel convertToTriggerFormInputModel(Map<String, Object> triggerFormInput);

    default String mapToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    default Boolean mapToBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }

        throw new IllegalArgumentException("Unsupported type for boolean conversion: " + value.getClass()
            .getName());
    }

    @SuppressWarnings("unchecked")
    default List<FieldOptionModel> mapToFieldOptionModelList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> convertToFieldOptionModel((Map<String, Object>) item))
                .toList();
        }

        throw new IllegalArgumentException(
            "Unsupported type for field option list conversion: " + value.getClass()
                .getName());
    }

    @SuppressWarnings("unchecked")
    default List<TriggerFormInputModel> mapToTriggerFormInputModelList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> convertToTriggerFormInputModel((Map<String, Object>) item))
                .toList();
        }

        throw new IllegalArgumentException(
            "Unsupported type for trigger form input list conversion: " + value.getClass()
                .getName());
    }

    default Integer mapToInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String string) {
            return Integer.parseInt(string);
        }

        throw new IllegalArgumentException("Unsupported type for integer conversion: " + value.getClass()
            .getName());
    }
}
