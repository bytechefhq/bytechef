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

package com.bytechef.component.chat.cluster;

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.ai.approval.ApprovalRequestEvents;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ChatApprovalChannel {

    private ChatApprovalChannel() {
    }

    public static ClusterElementDefinition<ApprovalChannelFunction> of(
        MessageBroker messageBroker, JobService jobService) {

        return ComponentDsl.<ApprovalChannelFunction>clusterElement("chat")
            .title("Chat")
            .description(
                "Delivers the approval request as an inline card in the chat conversation that started the workflow " +
                    "run. Pair with a fallback channel for runs not started from chat.")
            .type(APPROVAL_CHANNELS)
            .object(
                () -> (inputParameters, connectionParameters, formUrl, context) -> perform(
                    inputParameters, formUrl, context, messageBroker, jobService));
    }

    static Object perform(
        Parameters inputParameters, String formUrl, ClusterElementContext context, MessageBroker messageBroker,
        JobService jobService) {

        Long jobId = ((ClusterElementContextAware) context).getJobId();

        if (jobId == null || !isStreamedExecution(jobService, jobId)) {
            throw new IllegalStateException(
                "The chat approval channel requires a run with a chat origin, but this run was not started from a " +
                    "chat conversation. Configure a fallback approval channel for runs that are not started from " +
                    "chat.");
        }

        Map<String, Object> eventData = ApprovalRequestEvents.buildApprovalRequestEventData(inputParameters, formUrl);

        SseStreamEvent sseStreamEvent = new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_DATA, eventData);

        sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);

        return null;
    }

    private static boolean isStreamedExecution(JobService jobService, long jobId) {
        Job job = jobService.getJob(jobId);

        return MapUtils.getBoolean(job.getMetadata(), MetadataConstants.STREAMED_EXECUTION, false);
    }
}
