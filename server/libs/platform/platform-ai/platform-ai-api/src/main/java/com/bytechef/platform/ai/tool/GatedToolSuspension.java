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

package com.bytechef.platform.ai.tool;

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;

import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record GatedToolSuspension(String toolName, String toolInput, @Nullable ApprovalRequest approvalRequest) {

    static final String FORM_URL = "formUrl";

    public GatedToolSuspension {
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(toolInput, "toolInput");
    }

    public GatedToolSuspension(String toolName, String toolInput) {
        this(toolName, toolInput, null);
    }

    public static Optional<GatedToolSuspension> from(Map<String, ?> continueParameters) {
        Object toolName = continueParameters.get(ToolSuspendConstants.GATED_TOOL_NAME);

        if (toolName == null) {
            return Optional.empty();
        }

        Object toolInput = continueParameters.get(ToolSuspendConstants.GATED_TOOL_INPUT);

        if (toolInput == null) {
            throw new IllegalStateException(
                "The suspended call of the gated tool '" + toolName + "' has no stored input");
        }

        return Optional.of(
            new GatedToolSuspension(toolName.toString(), toolInput.toString(),
                ApprovalRequest.from(continueParameters)));
    }

    public Map<String, Object> toContinueParameters() {
        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(ToolSuspendConstants.GATED_TOOL_NAME, toolName);
        continueParameters.put(ToolSuspendConstants.GATED_TOOL_INPUT, toolInput);

        if (approvalRequest != null) {
            approvalRequest.putContinueParameters(continueParameters);
        }

        return continueParameters;
    }

    public record ApprovalRequest(String formTitle, String formDescription, String formUrl, Instant raisedAt) {

        public ApprovalRequest {
            Objects.requireNonNull(formTitle, "formTitle");
            Objects.requireNonNull(formDescription, "formDescription");
            Objects.requireNonNull(formUrl, "formUrl");
            Objects.requireNonNull(raisedAt, "raisedAt");
        }

        private static @Nullable ApprovalRequest from(Map<String, ?> continueParameters) {
            if (!(continueParameters.get(FORM_URL) instanceof String formUrl) ||
                !(continueParameters.get(MetadataConstants.APPROVAL_RAISED_AT) instanceof String raisedAt) ||
                !(continueParameters
                    .get(MetadataConstants.APPROVAL_FORM_PARAMETERS) instanceof Map<?, ?> formParameters)
                ||
                !(formParameters.get(FORM_TITLE) instanceof String formTitle) ||
                !(formParameters.get(FORM_DESCRIPTION) instanceof String formDescription)) {

                return null;
            }

            try {
                return new ApprovalRequest(formTitle, formDescription, formUrl, Instant.parse(raisedAt));
            } catch (DateTimeParseException dateTimeParseException) {
                return null;
            }
        }

        private void putContinueParameters(Map<String, Object> continueParameters) {
            continueParameters.put(FORM_URL, formUrl);
            continueParameters.put(MetadataConstants.APPROVAL_RAISED_AT, raisedAt.toString());
            continueParameters.put(MetadataConstants.APPROVAL_RESUME, true);
            continueParameters.put(
                MetadataConstants.APPROVAL_FORM_PARAMETERS,
                Map.of(FORM_TITLE, formTitle, FORM_DESCRIPTION, formDescription));
        }
    }
}
