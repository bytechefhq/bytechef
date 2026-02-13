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

package com.bytechef.component.slack.action;

import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class SlackSendApprovalMessageActionTest extends AbstractSlackActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @Test
    void testPerform() {
        Approval.Links links = new Approval.Links("approve", "disapprove");

        when(mockedActionContext.approval(any()))
            .thenReturn(links);

        Object result = SlackSendApprovalMessageAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedObject, result);
        assertEquals(
            List.of("channel", "%s%n%n Approve: ${approvalLink}%n%n Disapprove: ${disapprovalLink}".formatted("text")),
            stringArgumentCaptor.getAllValues());
        assertEquals(mockedActionContext, contextArgumentCaptor.getValue());

        List<Map<String, Object>> expectedBlocks = List.of(
            Map.of(
                "type", "section", TEXT,
                Map.of("type", "mrkdwn", TEXT, "text")),
            Map.of(
                "type", "actions", "block_id", "actions", "elements",
                List.of(
                    Map.of(
                        "type", "button", "text", Map.of("type", "plain_text", "text", "Approve"),
                        "style", "primary", "url", links.approvalLink()),
                    Map.of(
                        "type", "button", "text", Map.of("type", "plain_text", "text", "Disapprove"),
                        "style", "danger", "url", links.disapprovalLink()))));

        assertEquals(expectedBlocks, listArgumentCaptor.getValue());
    }
}
