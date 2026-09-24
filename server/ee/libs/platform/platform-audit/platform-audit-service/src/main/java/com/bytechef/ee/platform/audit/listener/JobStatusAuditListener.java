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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String SYSTEM_PRINCIPAL = "SYSTEM";

    private static final Logger log = LoggerFactory.getLogger(JobStatusAuditListener.class);

    private final AuditEventService auditEventService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public JobStatusAuditListener(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        Job.Status status = jobStatusApplicationEvent.getStatus();

        Map<String, String> data = new LinkedHashMap<>();

        data.put("jobId", String.valueOf(jobStatusApplicationEvent.getJobId()));
        data.put("status", status.name());

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType("JOB_" + status.name());
        persistentAuditEvent.setPrincipal(SYSTEM_PRINCIPAL);
        persistentAuditEvent.setData(data);

        try {
            auditEventService.saveAll(List.of(persistentAuditEvent));
        } catch (Exception exception) {
            log.error(
                "AUDIT PERSISTENCE FAILURE: the job status event was not recorded \u2014 audit trail is incomplete: {}",
                persistentAuditEvent, exception);
        }
    }
}
