/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.ApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.ErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerStartedApplicationEvent;
import com.bytechef.platform.workflow.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.platform.workflow.coordinator.event.listener.ErrorEventListener;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
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
@ConditionalOnBooleanProperty("bytechef.audit.enabled")
@Order(Ordered.LOWEST_PRECEDENCE)
public class TriggerAuditListener implements ApplicationEventListener, ErrorEventListener {

    private static final int ERROR_MAX_LENGTH = 256;
    private static final String SYSTEM_PRINCIPAL = "SYSTEM";
    private static final String TRIGGER_FAILED = "TRIGGER_FAILED";
    private static final String TRIGGER_STARTED = "TRIGGER_STARTED";

    private static final Logger log = LoggerFactory.getLogger(TriggerAuditListener.class);

    private final AuditEventService auditEventService;
    private final ObjectProvider<TriggerExecutionService> triggerExecutionServiceProvider;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public TriggerAuditListener(
        AuditEventService auditEventService, ObjectProvider<TriggerExecutionService> triggerExecutionServiceProvider) {

        this.auditEventService = auditEventService;
        this.triggerExecutionServiceProvider = triggerExecutionServiceProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof TriggerStartedApplicationEvent triggerStartedApplicationEvent)) {
            return;
        }

        long triggerExecutionId = triggerStartedApplicationEvent.getTriggerExecutionId();

        Map<String, String> data = new LinkedHashMap<>();

        data.put("triggerExecutionId", String.valueOf(triggerExecutionId));

        saveAuditEvent(TRIGGER_STARTED, data, fetchTriggerExecution(triggerExecutionId));
    }

    @Override
    public void onErrorEvent(ErrorEvent errorEvent) {
        if (!(errorEvent instanceof TriggerExecutionErrorEvent triggerExecutionErrorEvent)) {
            return;
        }

        TriggerExecution triggerExecution = triggerExecutionErrorEvent.getTriggerExecution();

        Map<String, String> data = new LinkedHashMap<>();

        Long triggerExecutionId = triggerExecution.getId();

        if (triggerExecutionId != null) {
            data.put("triggerExecutionId", String.valueOf(triggerExecutionId));
        }

        ExecutionError executionError = triggerExecutionErrorEvent.getError();

        if (executionError != null) {
            String message = executionError.getMessage();

            if (message != null) {
                data.put("error", truncate(message));
            }
        }

        saveAuditEvent(TRIGGER_FAILED, data, triggerExecution);
    }

    private @Nullable TriggerExecution fetchTriggerExecution(long triggerExecutionId) {
        try {
            TriggerExecutionService triggerExecutionService = triggerExecutionServiceProvider.getIfAvailable();

            if (triggerExecutionService == null) {
                return null;
            }

            return triggerExecutionService.getTriggerExecution(triggerExecutionId);
        } catch (Exception exception) {
            log.warn(
                "The trigger execution {} could not be resolved; its audit event is recorded without the workflow",
                triggerExecutionId, exception);

            return null;
        }
    }

    private void saveAuditEvent(
        String eventType, Map<String, String> data, @Nullable TriggerExecution triggerExecution) {

        PlatformType platform = null;

        if (triggerExecution != null) {
            WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

            if (workflowExecutionId != null) {
                platform = workflowExecutionId.getType();

                data.put("principalId", String.valueOf(workflowExecutionId.getJobPrincipalId()));
                data.put("workflowUuid", workflowExecutionId.getWorkflowUuid());
                data.put("triggerName", workflowExecutionId.getTriggerName());
            }
        }

        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setPlatform(platform);
        persistentAuditEvent.setPrincipal(SYSTEM_PRINCIPAL);
        persistentAuditEvent.setData(data);

        try {
            auditEventService.saveAll(List.of(persistentAuditEvent));
        } catch (Exception exception) {
            log.error(
                "AUDIT PERSISTENCE FAILURE: the trigger event was not recorded — audit trail is incomplete: {}",
                persistentAuditEvent, exception);
        }
    }

    private static String truncate(String value) {
        if (value.length() <= ERROR_MAX_LENGTH) {
            return value;
        }

        return value.substring(0, ERROR_MAX_LENGTH);
    }
}
