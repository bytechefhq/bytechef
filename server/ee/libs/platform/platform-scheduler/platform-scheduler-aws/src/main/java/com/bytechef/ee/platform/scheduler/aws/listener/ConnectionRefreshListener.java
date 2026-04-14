package com.bytechef.ee.platform.scheduler.aws.listener;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.CONNECTION_REFRESH;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsConnectionRefreshSchedulerConstants.SCHEDULER_CONNECTION_REFRESH_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SPLITTER;

/**
 * @version ee
 *
 * @author Nikolina Spehar
 */
public class ConnectionRefreshListener {

    private final ConnectionDefinitionFacade remoteConnectionDefinitionFacade;
    private final SchedulerClient schedulerClient;

    @SuppressFBWarnings("EI")
    public ConnectionRefreshListener(
        SchedulerClient schedulerClient, ConnectionDefinitionFacade remoteConnectionDefinitionFacade) {

        this.schedulerClient = schedulerClient;
        this.remoteConnectionDefinitionFacade = remoteConnectionDefinitionFacade;
    }

    @SqsListener(SCHEDULER_CONNECTION_REFRESH_QUEUE)
    public void onSchedule(String message) {
        String[] split = message.split(SPLITTER);
        String tenantId = split[0];
        Long connectionId = Long.valueOf(split[1]);

        refreshConnection(tenantId, connectionId);
    }

    private Instant refreshConnection(String tenantId, Long connectionId) {
        Instant connectionExpiry = null;

        ComponentConnection componentConnection = remoteConnectionDefinitionFacade.executeConnectionRefresh(
            tenantId, connectionId);

        if (componentConnection != null) {
            Map<String, ?> parameters = componentConnection.getParameters();

            Long expiresIn = (Long) parameters.get("expires_in");

            connectionExpiry = Instant.now()
                .plusSeconds(expiresIn);
        }

        return connectionExpiry;
    }
}
