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

package com.bytechef.platform.workflow.execution.web.rest.mapper;

import com.bytechef.platform.configuration.web.rest.model.FieldOptionModel;
import com.bytechef.platform.workflow.execution.web.rest.mapper.config.PlatformWorkflowExecutionMapperSpringConfig;
import com.bytechef.platform.workflow.execution.web.rest.model.ApprovalFormInputModel;
import com.bytechef.platform.workflow.execution.web.rest.model.ApprovalFormModel;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
public interface ApprovalFormMapper extends Converter<Map<String, Object>, ApprovalFormModel> {

    @Override
    ApprovalFormModel convert(Map<String, Object> approvalForm);

    FieldOptionModel convertToFieldOptionModel(Map<String, Object> fieldOption);

    ApprovalFormInputModel convertToApprovalFormInputModel(Map<String, Object> triggerFormInput);

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
    default List<ApprovalFormInputModel> mapToApprovalFormInputModelList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> convertToApprovalFormInputModel((Map<String, Object>) item))
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

    default Long mapToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string) {
            return Long.parseLong(string);
        }

        throw new IllegalArgumentException("Unsupported type for long conversion: " + value.getClass()
            .getName());
    }
}
