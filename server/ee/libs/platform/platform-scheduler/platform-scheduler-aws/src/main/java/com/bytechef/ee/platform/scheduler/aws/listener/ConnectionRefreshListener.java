/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.CONNECTION_REFRESH_LISTENER_ID;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.SCHEDULER_CONNECTION_REFRESH_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SPLITTER;

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.util.InactiveTenants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Nikolina Spehar
 */
public class ConnectionRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(ConnectionRefreshListener.class);

    private final ConnectionFacade connectionFacade;
    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

    @SuppressFBWarnings("EI")
    public ConnectionRefreshListener(
        ConnectionFacade connectionFacade, ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider) {

        this.connectionFacade = connectionFacade;
        this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
    }

    @SqsListener(
        queueNames = SCHEDULER_CONNECTION_REFRESH_QUEUE,
        id = CONNECTION_REFRESH_LISTENER_ID,
        factory = SCHEDULER_SQS_LISTENER_CONTAINER_FACTORY)
    public void onSchedule(String message) {
        String[] split = message.split(SPLITTER);
        String tenantId = split[0];
        Long connectionId = Long.valueOf(split[1]);

        if (InactiveTenants.isInactive(tenantDeletionServiceProvider, tenantId)) {
            log.debug("Skipping connection refresh for inactive tenant {}", tenantId);

            return;
        }

        TenantContext.runWithTenantId(tenantId, () -> connectionFacade.executeConnectionRefresh(connectionId));
    }
}
