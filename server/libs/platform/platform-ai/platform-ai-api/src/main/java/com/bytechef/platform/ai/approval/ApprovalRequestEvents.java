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

package com.bytechef.platform.ai.approval;

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_ID;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.RESUME_URL;
import static com.bytechef.platform.ai.constant.AiAgentSseEventType.APPROVAL_REQUEST;
import static com.bytechef.platform.ai.constant.AiAgentSseEventType.EVENT_TYPE;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class ApprovalRequestEvents {

    private static final String FORM_URL = "formUrl";

    private ApprovalRequestEvents() {
    }

    public static Map<String, Object> buildApprovalRequestEventData(Parameters inputParameters, String formUrl) {
        Map<String, Object> eventData = new LinkedHashMap<>();

        eventData.put(EVENT_TYPE, APPROVAL_REQUEST);
        eventData.put(RESUME_ID, inputParameters.getRequiredString(RESUME_ID));
        eventData.put(FORM_URL, formUrl);

        String resumeUrl = inputParameters.getString(RESUME_URL);

        if (resumeUrl != null) {
            eventData.put(RESUME_URL, resumeUrl);
        }

        String formTitle = inputParameters.getString(FORM_TITLE);

        if (formTitle != null) {
            eventData.put(FORM_TITLE, formTitle);
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        if (formDescription != null) {
            eventData.put(FORM_DESCRIPTION, formDescription);
        }

        String expiresAt = inputParameters.getString(EXPIRES_AT);

        if (expiresAt != null) {
            eventData.put(EXPIRES_AT, expiresAt);
        }

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        eventData.put(INPUTS, inputs);

        return eventData;
    }
}
