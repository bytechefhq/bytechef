/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.agent.AiHubScheduledChatDispatcher;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
class AgentScheduleFiredEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentScheduleFiredEventListener.class);

    private final AiHubPersonalAgentScheduleService scheduleService;
    private final AiHubTaskService taskService;
    private final AiHubScheduledChatDispatcher dispatcher;
    private final AiHubAuditPublisher auditPublisher;

    AgentScheduleFiredEventListener(
        AiHubPersonalAgentScheduleService scheduleService, AiHubTaskService taskService,
        AiHubScheduledChatDispatcher dispatcher, AiHubAuditPublisher auditPublisher) {

        this.scheduleService = scheduleService;
        this.taskService = taskService;
        this.dispatcher = dispatcher;
        this.auditPublisher = auditPublisher;
    }

    @EventListener
    void onFired(AgentScheduleFiredEvent event) {
        long scheduleId = event.agentScheduleId();

        AiHubPersonalAgentSchedule schedule = scheduleService.findEnabled(scheduleId);

        if (schedule == null) {
            log.debug("Schedule {} missing or disabled — skipping fire", scheduleId);

            return;
        }

        try {
            AiHubTask task = taskService.createAiHubPersonalAgentChat(
                schedule.getWorkspaceId(),
                schedule.getUserId(),
                schedule.getEnvironment()
                    .ordinal(),
                schedule.getAiHubPersonalAgentId(),
                schedule.getTitle());

            dispatcher.dispatch(
                schedule.getUserId(),
                schedule.getWorkspaceId(),
                schedule.getEnvironment()
                    .ordinal(),
                schedule.getAiHubPersonalAgentId(),
                task.getThreadId(),
                schedule.getPrompt());

            scheduleService.recordFire(scheduleId);

            log.info(
                "Scheduled fire ok: scheduleId={}, agentId={}, taskId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), task.getId());

            auditPublisher.publish(
                AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED,
                Map.of(
                    "workspaceId", String.valueOf(schedule.getWorkspaceId()),
                    "agentId", String.valueOf(schedule.getAiHubPersonalAgentId()),
                    "scheduleId", String.valueOf(scheduleId),
                    "taskId", String.valueOf(task.getId())));
        } catch (Exception e) {
            log.warn(
                "Scheduled fire failed: scheduleId={}, agentId={}, workspaceId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), schedule.getWorkspaceId(), e);

            scheduleService.recordFailure(scheduleId);
        }
    }
}
