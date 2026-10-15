/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Drives the listener against a real database: each job status transition it receives must leave one readable audit
 * row.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class JobStatusAuditListenerIntTest {

    @Autowired
    private JobStatusAuditListener jobStatusAuditListener;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testJobLifecycleIsReadableFromTheAuditTable() {
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.CREATED));
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.STARTED));
        jobStatusAuditListener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.COMPLETED));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository
            .findAllFiltered(new AuditEventFilter(null, null, null, null, "7"), PageRequest.of(0, 25));

        List<String> eventTypes = page.getContent()
            .stream()
            .map(PersistentAuditEvent::getEventType)
            .toList();

        assertThat(eventTypes).containsExactlyInAnyOrder("JOB_CREATED", "JOB_STARTED", "JOB_COMPLETED");
        assertThat(page.getContent())
            .allSatisfy(persistentAuditEvent -> assertThat(persistentAuditEvent.getData())
                .containsEntry("jobId", "7"));
    }
}
