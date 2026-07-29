/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationLogRetentionJobTest {

    @Test
    void testPurgeDeletesRowsOlderThanRetentionWindow() {
        ToolInvocationLogService toolInvocationLogService = mock(ToolInvocationLogService.class);

        when(toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(any())).thenReturn(3);

        ToolInvocationLogRetentionJob retentionJob = new ToolInvocationLogRetentionJob(90, toolInvocationLogService);

        Instant before = Instant.now()
            .minus(90, ChronoUnit.DAYS);

        retentionJob.purgeExpiredLogs();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);

        verify(toolInvocationLogService).deleteToolInvocationLogsCreatedBefore(captor.capture());

        Instant cutoff = captor.getValue();

        assertThat(cutoff).isBetween(before.minusSeconds(60), before.plusSeconds(60));
    }
}
