/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import java.text.ParseException;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;
import org.quartz.CronExpression;

/**
 * Stateless helper that translates the structured frequency fields of {@link AiHubPersonalAgentSchedule} into a 6-field
 * Quartz cron expression (seconds-resolution). {@code CUSTOM_CRON} is validated and passed through.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScheduleCronNormalizer {

    private ScheduleCronNormalizer() {
    }

    static String normalize(AiHubPersonalAgentSchedule schedule) {
        ScheduleFrequencyKind kind = schedule.getFrequencyKind();

        String cron = switch (kind) {
            case EVERY_X_MINUTES -> everyXMinutes(schedule.getIntervalMinutes());
            case HOURLY -> hourly(schedule.getMinuteOfHour());
            case DAILY -> daily(schedule.getTimeOfDay());
            case WEEKLY -> weekly(schedule.getTimeOfDay(), schedule.getDayOfWeek());
            case MONTHLY -> monthly(schedule.getTimeOfDay(), schedule.getDayOfMonth());
            case CUSTOM_CRON -> customCron(schedule.getCronExpression());
        };

        validate(cron);

        return cron;
    }

    private static String everyXMinutes(Integer interval) {
        require(interval != null && interval >= 1 && interval <= 59,
            "intervalMinutes must be 1..59");

        return "0 0/" + interval + " * * * ?";
    }

    private static String hourly(Integer minute) {
        require(minute != null && minute >= 0 && minute <= 59,
            "minuteOfHour must be 0..59");

        return "0 " + minute + " * * * ?";
    }

    private static String daily(@Nullable LocalTime time) {
        if (time == null) {
            throw new IllegalArgumentException("timeOfDay required for DAILY");
        }

        return "0 " + time.getMinute() + " " + time.getHour() + " * * ?";
    }

    private static String weekly(@Nullable LocalTime time, @Nullable Integer isoDayOfWeek) {
        if (time == null) {
            throw new IllegalArgumentException("timeOfDay required for WEEKLY");
        }
        if (isoDayOfWeek == null || isoDayOfWeek < 1 || isoDayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1..7 (ISO)");
        }

        // ISO: 1=Mon … 7=Sun. Quartz: 1=Sun … 7=Sat. Convert: quartz = (iso % 7) + 1.
        int quartzDay = (isoDayOfWeek % 7) + 1;

        return "0 " + time.getMinute() + " " + time.getHour() + " ? * " + quartzDay;
    }

    private static String monthly(@Nullable LocalTime time, @Nullable Integer dayOfMonth) {
        if (time == null) {
            throw new IllegalArgumentException("timeOfDay required for MONTHLY");
        }

        require(dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31,
            "dayOfMonth must be 1..31");

        return "0 " + time.getMinute() + " " + time.getHour() + " " + dayOfMonth + " * ?";
    }

    private static String customCron(@Nullable String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("cronExpression required for CUSTOM_CRON");
        }

        return expression.trim();
    }

    private static void validate(String cron) {
        try {
            new CronExpression(cron);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
