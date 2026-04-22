/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.SCHEDULER_CONNECTION_REFRESH_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SPLITTER;

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.annotation.SqsListener;

/**
 * @version ee
 *
 * @author Nikolina Spehar
 */
public class ConnectionRefreshListener {

    private final ConnectionFacade connectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionRefreshListener(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @SqsListener(SCHEDULER_CONNECTION_REFRESH_QUEUE)
    public void onSchedule(String message) {
        String[] split = message.split(SPLITTER);
        String tenantId = split[0];
        Long connectionId = Long.valueOf(split[1]);

        TenantContext.runWithTenantId(tenantId, () -> connectionFacade.executeConnectionRefresh(connectionId));
    }
}
