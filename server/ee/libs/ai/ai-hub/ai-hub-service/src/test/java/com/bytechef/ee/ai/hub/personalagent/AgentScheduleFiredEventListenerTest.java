/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.agent.AiHubScheduledChatDispatcher;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgentScheduleFiredEventListenerTest {

    private AiHubPersonalAgentScheduleService scheduleService;
    private AiHubTaskService taskService;
    private AiHubScheduledChatDispatcher dispatcher;
    private AiHubAuditPublisher auditPublisher;
    private AgentScheduleFiredEventListener listener;

    @BeforeEach
    void setUp() {
        scheduleService = Mockito.mock(AiHubPersonalAgentScheduleService.class);
        taskService = Mockito.mock(AiHubTaskService.class);
        dispatcher = Mockito.mock(AiHubScheduledChatDispatcher.class);
        auditPublisher = Mockito.mock(AiHubAuditPublisher.class);
        listener = new AgentScheduleFiredEventListener(scheduleService, taskService, dispatcher, auditPublisher);
    }

    @Test
    void testHappyPathCreatesTaskAndDispatches() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        AiHubTask task = mockTaskWithThread("thread-uuid");
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            eq(1L), eq(2L), anyInt(), eq(100L), eq("Daily report")))
                .thenReturn(task);

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(taskService).createAiHubPersonalAgentChat(1L, 2L, 0, 100L, "Daily report");
        verify(dispatcher).dispatch(2L, 1L, 0, 100L, "thread-uuid", "Summarize yesterday");
        verify(scheduleService).recordFire(7L);
        verify(scheduleService, never()).recordFailure(anyLong());
    }

    @Test
    void testDisabledOrMissingScheduleSkips() {
        when(scheduleService.findEnabled(7L)).thenReturn(null);

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(taskService, never()).createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any());
        verify(dispatcher, never()).dispatch(
            anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());
        verify(scheduleService, never()).recordFire(anyLong());
    }

    @Test
    void testDispatchFailureRecordsFailure() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        AiHubTask task = mockTaskWithThread("thread-uuid");
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any())).thenReturn(task);
        Mockito.doThrow(new RuntimeException("boom"))
            .when(dispatcher)
            .dispatch(anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(scheduleService).recordFailure(7L);
        verify(scheduleService, never()).recordFire(anyLong());
    }

    @Test
    void testTaskCreationFailureRecordsFailure() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any()))
                .thenThrow(new RuntimeException("DB down"));

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(scheduleService).recordFailure(7L);
        verify(dispatcher, never()).dispatch(
            anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());
    }

    @Test
    void testOnFiredSuccessPublishesAuditEvent() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        AiHubTask task = mockTaskWithThread("thread-uuid");
        when(task.getId()).thenReturn(123L);
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            eq(1L), eq(2L), anyInt(), eq(100L), eq("Daily report")))
                .thenReturn(task);

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED),
            argThat(data -> data.get("taskId") != null && data.get("scheduleId") != null));
    }

    private AiHubPersonalAgentSchedule buildEnabledSchedule(long id) {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setId(id);
        schedule.setAiHubPersonalAgentId(100L);
        schedule.setWorkspaceId(1L);
        schedule.setUserId(2L);
        schedule.setEnvironment(com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT);
        schedule.setTitle("Daily report");
        schedule.setPrompt("Summarize yesterday");
        return schedule;
    }

    private AiHubTask mockTaskWithThread(String threadId) {
        AiHubTask task = Mockito.mock(AiHubTask.class);
        when(task.getThreadId()).thenReturn(threadId);
        return task;
    }
}
