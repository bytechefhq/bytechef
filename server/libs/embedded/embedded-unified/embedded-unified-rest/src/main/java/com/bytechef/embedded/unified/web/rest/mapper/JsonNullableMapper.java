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

package com.bytechef.embedded.unified.web.rest.mapper;

import com.bytechef.embedded.unified.web.rest.mapper.config.UnifiedConfigurationMapperSpringConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.mapstruct.Mapper;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(
    config = UnifiedConfigurationMapperSpringConfig.class,
    implementationName = "EmbeddedUnifiedJsonNullableMapper")
public interface JsonNullableMapper {

    default JsonNullable<BigDecimal> map(BigDecimal value) {
        return JsonNullable.of(value);
    }

    default JsonNullable<Integer> map(int value) {
        return JsonNullable.of(value);
    }

    default JsonNullable<LocalDateTime> map(LocalDateTime value) {
        return JsonNullable.of(value);
    }

    default JsonNullable<String> map(String value) {
        return JsonNullable.of(value);
    }

    default JsonNullable<Object> map(Map<String, ?> value) {
        return JsonNullable.of(value);
    }

    default BigDecimal mapBigDecimal(JsonNullable<BigDecimal> value) {
        return value.orElse(null);
    }

    default int mapInteger(JsonNullable<Integer> value) {
        return value.orElse(null);
    }

    default LocalDateTime mapLocalDateTime(JsonNullable<LocalDateTime> value) {
        return value.orElse(null);
    }

    default String mapString(JsonNullable<String> value) {
        return value.orElse(null);
    }
}
