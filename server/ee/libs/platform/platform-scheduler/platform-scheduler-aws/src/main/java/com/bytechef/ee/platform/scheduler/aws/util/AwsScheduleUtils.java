/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.ListSchedulesRequest;
import software.amazon.awssdk.services.scheduler.model.ListSchedulesResponse;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.ScheduleSummary;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AwsScheduleUtils {

    private static final Logger log = LoggerFactory.getLogger(AwsScheduleUtils.class);

    private AwsScheduleUtils() {
    }

    /**
     * Deletes every schedule of the group whose name starts with the prefix and matches the filter.
     *
     * @return the names of schedules that could not be listed or deleted, empty when everything succeeded
     */
    public static List<String> deleteSchedules(
        SchedulerClient schedulerClient, String groupName, String namePrefix, Predicate<String> nameFilter) {

        List<String> scheduleNames;

        try {
            scheduleNames = listScheduleNames(schedulerClient, groupName, namePrefix);
        } catch (RuntimeException e) {
            log.error("Unable to list schedules of group: {}", groupName, e);

            return List.of(groupName);
        }

        List<String> failures = new ArrayList<>();

        for (String scheduleName : scheduleNames) {
            if (!nameFilter.test(scheduleName)) {
                continue;
            }

            try {
                schedulerClient.deleteSchedule(
                    DeleteScheduleRequest.builder()
                        .groupName(groupName)
                        .name(scheduleName)
                        .build());
            } catch (ResourceNotFoundException e) {
                log.debug("Schedule already deleted: {}/{}", groupName, scheduleName);
            } catch (RuntimeException e) {
                log.error("Unable to delete schedule: {}/{}", groupName, scheduleName, e);

                failures.add(groupName + "/" + scheduleName);
            }
        }

        return failures;
    }

    private static List<String> listScheduleNames(
        SchedulerClient schedulerClient, String groupName, String namePrefix) {

        List<String> scheduleNames = new ArrayList<>();
        String nextToken = null;

        do {
            ListSchedulesResponse listSchedulesResponse;

            try {
                listSchedulesResponse = schedulerClient.listSchedules(
                    ListSchedulesRequest.builder()
                        .groupName(groupName)
                        .namePrefix(namePrefix)
                        .nextToken(nextToken)
                        .build());
            } catch (ResourceNotFoundException e) {
                log.debug("Schedule group does not exist: {}", groupName);

                return List.of();
            }

            for (ScheduleSummary scheduleSummary : listSchedulesResponse.schedules()) {
                scheduleNames.add(scheduleSummary.name());
            }

            nextToken = listSchedulesResponse.nextToken();
        } while (nextToken != null);

        return scheduleNames;
    }
}
