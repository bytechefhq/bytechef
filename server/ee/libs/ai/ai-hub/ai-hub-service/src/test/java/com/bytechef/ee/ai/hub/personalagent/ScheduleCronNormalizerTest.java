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

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ScheduleCronNormalizerTest {

    @Test
    void testEveryXMinutesProducesIntervalCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.EVERY_X_MINUTES);
        schedule.setIntervalMinutes(15);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0/15 * * * ?");
    }

    @Test
    void testHourlyProducesMinuteOfHourCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.HOURLY);
        schedule.setMinuteOfHour(30);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 30 * * * ?");
    }

    @Test
    void testDailyProducesTimeOfDayCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 9 * * ?");
    }

    @Test
    void testWeeklyProducesDayOfWeekCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.WEEKLY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setDayOfWeek(1); // Monday (ISO)

        String cron = ScheduleCronNormalizer.normalize(schedule);

        // Quartz day-of-week: 1=Sunday … 7=Saturday — normalizer converts ISO Mon(1) → Quartz 2
        assertThat(cron).isEqualTo("0 0 9 ? * 2");
    }

    @Test
    void testMonthlyProducesDayOfMonthCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.MONTHLY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setDayOfMonth(15);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 9 15 * ?");
    }

    @Test
    void testCustomCronPassesThroughAfterValidation() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.CUSTOM_CRON);
        schedule.setCronExpression("0 0 12 * * ?");

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }

    @Test
    void testInvalidCustomCronThrows() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.CUSTOM_CRON);
        schedule.setCronExpression("not a cron");

        assertThatThrownBy(() -> ScheduleCronNormalizer.normalize(schedule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cron");
    }

    @Test
    void testEveryXMinutesWithMissingIntervalThrows() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.EVERY_X_MINUTES);

        assertThatThrownBy(() -> ScheduleCronNormalizer.normalize(schedule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("intervalMinutes");
    }
}
