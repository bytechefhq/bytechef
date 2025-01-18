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

package com.bytechef.web.rest.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JsonNullableMapper {

    public JsonNullable<BigDecimal> map(BigDecimal value) {
        return JsonNullable.of(value);
    }

    public JsonNullable<Integer> map(int value) {
        return JsonNullable.of(value);
    }

    public JsonNullable<String> map(String value) {
        return JsonNullable.of(value);
    }

    public JsonNullable<Object> map(Map<String, ?> value) {
        return JsonNullable.of(value);
    }

    public JsonNullable<OffsetDateTime> map(Instant value) {
        return JsonNullable.of(OffsetDateTime.from(value));
    }

    public BigDecimal mapBigDecimal(JsonNullable<BigDecimal> value) {
        return value.orElse(null);
    }

    public Instant mapInstant(JsonNullable<OffsetDateTime> value) {
        OffsetDateTime offsetDateTime = value.get();

        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    public int mapInteger(JsonNullable<Integer> value) {
        return value.orElse(null);
    }

    public String mapString(JsonNullable<String> value) {
        return value.orElse(null);
    }
}
