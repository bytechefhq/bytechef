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

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public sealed interface GateDecision permits GateDecision.Approved, GateDecision.Denied {

    String APPROVED = "approved";

    String APPROVED_BY = "approvedBy";
    String COMMENT = "comment";

    /**
     * Returns whoever resolved the approval, whether it was approved or denied.
     *
     * @return the reviewer, or null when unknown
     */
    @Nullable
    String reviewer();

    @Nullable
    String comment();

    static GateDecision from(Map<String, ?> resumeData) {
        String reviewer = toNonBlankString(resumeData.get(APPROVED_BY));
        String comment = toNonBlankString(resumeData.get(COMMENT));

        if (isApproved(resumeData.get(APPROVED))) {
            return new Approved(reviewer, comment);
        }

        return new Denied(reviewer, comment);
    }

    private static boolean isApproved(@Nullable Object approvedValue) {
        if (approvedValue instanceof Boolean approved) {
            return approved;
        }

        return approvedValue instanceof String approvedString && Boolean.parseBoolean(approvedString);
    }

    private static @Nullable String toNonBlankString(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        String string = value.toString();

        return string.isBlank() ? null : string;
    }

    record Approved(@Nullable String reviewer, @Nullable String comment) implements GateDecision {
    }

    record Denied(@Nullable String reviewer, @Nullable String comment) implements GateDecision {
    }
}
