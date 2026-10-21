/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.ListSchedulesRequest;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.SchedulerException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AwsScheduleUtilsTest {

    private final SchedulerClient schedulerClient = mock(SchedulerClient.class);

    @Test
    void testDeleteSchedulesReturnsNoFailuresWhenGroupDoesNotExist() {
        when(schedulerClient.listSchedules(any(ListSchedulesRequest.class)))
            .thenThrow(ResourceNotFoundException.builder()
                .message("Schedule group ScheduleTrigger does not exist.")
                .build());

        assertThat(
            AwsScheduleUtils.deleteSchedules(schedulerClient, "ScheduleTrigger", "ScheduleTrigger", name -> true))
                .isEmpty();

        verify(schedulerClient, never()).deleteSchedule(any(DeleteScheduleRequest.class));
    }

    @Test
    void testDeleteSchedulesReportsGroupWhenListingFails() {
        when(schedulerClient.listSchedules(any(ListSchedulesRequest.class)))
            .thenThrow(SchedulerException.builder()
                .message("throttled")
                .build());

        assertThat(
            AwsScheduleUtils.deleteSchedules(schedulerClient, "ScheduleTrigger", "ScheduleTrigger", name -> true))
                .containsExactly("ScheduleTrigger");
    }
}
