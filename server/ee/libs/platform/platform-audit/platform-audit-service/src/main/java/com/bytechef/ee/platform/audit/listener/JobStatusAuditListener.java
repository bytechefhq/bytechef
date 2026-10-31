/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.SharedApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobStatusAuditListener implements SharedApplicationEventListener {

    private static final Duration JOB_CREATED_LOOKBACK = Duration.ofDays(1);
    private static final String SYSTEM_PRINCIPAL = "SYSTEM";

    private static final Logger log = LoggerFactory.getLogger(JobStatusAuditListener.class);

    private final AuditEventService auditEventService;
    private final ObjectProvider<PrincipalJobService> principalJobServiceProvider;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public JobStatusAuditListener(
        AuditEventService auditEventService, ObjectProvider<PrincipalJobService> principalJobServiceProvider) {

        this.auditEventService = auditEventService;
        this.principalJobServiceProvider = principalJobServiceProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        long jobId = jobStatusApplicationEvent.getJobId();
        Job.Status status = jobStatusApplicationEvent.getStatus();

        Map<String, String> data = new LinkedHashMap<>();

        data.put("jobId", String.valueOf(jobId));
        data.put("status", status.name());

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        LocalDateTime eventDate = LocalDateTime.now();
        PlatformType platform = resolvePlatform(jobId);

        persistentAuditEvent.setEventDate(eventDate);
        persistentAuditEvent.setEventType("JOB_" + status.name());
        persistentAuditEvent.setPlatform(platform);
        persistentAuditEvent.setPrincipal(SYSTEM_PRINCIPAL);
        persistentAuditEvent.setData(data);

        try {
            auditEventService.saveAll(List.of(persistentAuditEvent));
        } catch (Exception exception) {
            log.error(
                "AUDIT PERSISTENCE FAILURE: the job status event was not recorded \u2014 audit trail is incomplete: {}",
                persistentAuditEvent, exception);
        }

        if (platform != null && status != Job.Status.CREATED) {
            updateJobCreatedPlatform(jobId, platform, eventDate.minus(JOB_CREATED_LOOKBACK));
        }
    }

    private void updateJobCreatedPlatform(long jobId, PlatformType platform, LocalDateTime since) {
        try {
            auditEventService.updateJobCreatedPlatform(jobId, platform, since);
        } catch (Exception exception) {
            log.warn("The platform of the created event of job {} could not be updated", jobId, exception);
        }
    }

    private @Nullable PlatformType resolvePlatform(long jobId) {
        try {
            PrincipalJobService principalJobService = principalJobServiceProvider.getIfAvailable();

            if (principalJobService == null) {
                return null;
            }

            for (PlatformType platformType : PlatformType.values()) {
                Optional<Long> principalIdOptional = principalJobService.fetchJobPrincipalId(jobId, platformType);

                if (principalIdOptional.isPresent()) {
                    return platformType;
                }
            }
        } catch (Exception exception) {
            log.warn("The platform of job {} could not be resolved; its audit event is recorded without it", jobId,
                exception);
        }

        return null;
    }
}
