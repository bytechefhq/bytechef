/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hands the trigger output of a subscription's hidden workflow to {@link TriggerSubscriptionEventSender}. Registered
 * under its task type so the worker's task handler registry resolves it by bean name. Delivers only for a job of the
 * subscription's own project deployment, so another workflow that uses this task type cannot send events for it.
 * <p>
 * The trigger is registered before the ENABLED status commits, so a trigger that fires at once, such as the first poll
 * of a polling trigger, can run the deliver task while the subscription is still being enabled. An event that finds the
 * subscription not ENABLED while a request holds its registration lease throws
 * {@link TriggerSubscriptionEnablingException} instead of being dropped; the deliver task retries, as the workflow
 * definition configures it to, and delivers once the subscription is ENABLED, or drops the event once the enable ended
 * without enabling it and released the lease. Throwing hands the wait to the coordinator, so no worker thread waits.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("triggerSubscription/v1/deliver")
@ConditionalOnEEVersion
public class TriggerSubscriptionDeliveryTaskHandler implements TaskHandler<Void> {

    private static final String OUTPUT = "output";
    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionDeliveryTaskHandler.class);

    private final PrincipalJobService principalJobService;
    private final TriggerSubscriptionEventSender triggerSubscriptionEventSender;
    private final TriggerSubscriptionService triggerSubscriptionService;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionDeliveryTaskHandler(
        PrincipalJobService principalJobService, TriggerSubscriptionEventSender triggerSubscriptionEventSender,
        TriggerSubscriptionService triggerSubscriptionService) {

        this.principalJobService = principalJobService;
        this.triggerSubscriptionEventSender = triggerSubscriptionEventSender;
        this.triggerSubscriptionService = triggerSubscriptionService;
    }

    @Override
    public Void handle(TaskExecution taskExecution) {
        try {
            deliver(taskExecution);
        } catch (TriggerSubscriptionEnablingException triggerSubscriptionEnablingException) {
            throw triggerSubscriptionEnablingException;
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to deliver trigger subscription event for job id={}", taskExecution.getJobId(),
                runtimeException);
        }

        return null;
    }

    private void deliver(TaskExecution taskExecution) {
        Map<String, ?> parameters = taskExecution.getParameters();

        long subscriptionId = MapUtils.getRequiredLong(parameters, SUBSCRIPTION_ID);

        Optional<TriggerSubscription> triggerSubscriptionOptional =
            triggerSubscriptionService.fetchTriggerSubscription(subscriptionId);

        if (triggerSubscriptionOptional.isEmpty()) {
            log.debug("Trigger subscription id={} not found, event not delivered", subscriptionId);

            return;
        }

        TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

        Long jobId = taskExecution.getJobId();

        if (triggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
            if (isRegistrationLeaseActive(triggerSubscription) && isSubscriptionJob(triggerSubscription, jobId)) {
                log.debug(
                    "Trigger subscription id={} is being enabled, event of job id={} held until enabling completes",
                    subscriptionId, jobId);

                throw new TriggerSubscriptionEnablingException(subscriptionId);
            }

            log.debug(
                "Trigger subscription id={} is {}, event not delivered", subscriptionId,
                triggerSubscription.getStatus());

            return;
        }

        if (!isSubscriptionJob(triggerSubscription, jobId)) {
            log.warn(
                "Job id={} does not belong to project deployment id={} of trigger subscription id={}, event not " +
                    "delivered",
                jobId, triggerSubscription.getProjectDeploymentId(), subscriptionId);

            return;
        }

        triggerSubscriptionEventSender.sendTriggerEvent(triggerSubscription, parameters.get(OUTPUT), jobId);
    }

    private static boolean isRegistrationLeaseActive(TriggerSubscription triggerSubscription) {
        Instant registrationLeaseUntil = triggerSubscription.getRegistrationLeaseUntil();

        return registrationLeaseUntil != null && registrationLeaseUntil.isAfter(Instant.now());
    }

    private boolean isSubscriptionJob(TriggerSubscription triggerSubscription, @Nullable Long jobId) {
        Long projectDeploymentId = triggerSubscription.getProjectDeploymentId();

        if (jobId == null || projectDeploymentId == null) {
            return false;
        }

        Optional<Long> jobPrincipalIdOptional = principalJobService.fetchJobPrincipalId(
            jobId, PlatformType.AUTOMATION);

        return jobPrincipalIdOptional.isPresent() && projectDeploymentId.equals(jobPrincipalIdOptional.get());
    }
}
