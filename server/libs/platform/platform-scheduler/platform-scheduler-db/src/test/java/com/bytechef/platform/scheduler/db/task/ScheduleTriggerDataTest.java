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

package com.bytechef.platform.scheduler.db.task;

import com.github.kagkarlsson.scheduler.boot.autoconfigure.Jackson3Serializer;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ScheduleTriggerDataTest {

    @Test
    void testScheduleUsesQuartzCronDialectInGivenZone() {
        ScheduleTriggerData data = new ScheduleTriggerData("0 0 9 * * ?", "Europe/Zagreb", "{}");

        Schedule schedule = data.getSchedule();

        Instant now = ZonedDateTime.of(2030, 6, 1, 8, 0, 0, 0, ZoneId.of("Europe/Zagreb"))
            .toInstant();
        Instant next = schedule.getInitialExecutionTime(now);

        Assertions.assertThat(next)
            .isEqualTo(ZonedDateTime.of(2030, 6, 1, 9, 0, 0, 0, ZoneId.of("Europe/Zagreb"))
                .toInstant());
    }

    @Test
    void testJacksonRoundTripIgnoresDerivedSchedule() {
        Jackson3Serializer serializer = new Jackson3Serializer(JsonMapper.builder()
            .build());
        ScheduleTriggerData data = new ScheduleTriggerData("0 * * * * ?", "UTC", "{\"expression\":\"* * * * *\"}");

        byte[] bytes = serializer.serialize(data);
        String json = new String(bytes, StandardCharsets.UTF_8);

        Assertions.assertThat(json)
            .contains("cronPattern")
            .doesNotContain("schedule");
        Assertions.assertThat(serializer.deserialize(ScheduleTriggerData.class, bytes))
            .isEqualTo(data);
    }
}
