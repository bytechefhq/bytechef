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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class DateTimeMapper {

    public Instant map(OffsetDateTime dateTime) {
        return dateTime == null ? null : Instant.from(dateTime);
    }

    public OffsetDateTime map(Instant instant) {
        if (instant == null) {
            return null;
        }

        ZoneId systemZone = ZoneId.systemDefault();
        ZoneRules zoneRules = systemZone.getRules();

        ZoneOffset zoneOffset = zoneRules.getOffset(instant);

        return instant.atOffset(zoneOffset);
    }

    public OffsetDateTime map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }
}
