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

package com.bytechef.platform.component.rule;

import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ComponentRuleEnforcer {

    @SuppressFBWarnings("EI")
    record ToolCall(
        String componentName, String toolName, String toolCallName, Map<String, ?> inputParameters,
        @Nullable Long connectionId, @Nullable Long jobId, @Nullable Long taskExecutionId,
        @Nullable String approvedBy, @Nullable Long jobPrincipalId, @Nullable PlatformType platformType) {
    }

    sealed interface Decision permits Decision.Allow, Decision.Block, Decision.RequireApproval {

        record Allow() implements Decision {
        }

        record Block(String reason) implements Decision {
        }

        @SuppressFBWarnings("EI")
        record RequireApproval(List<Long> ruleIds, String title, String description, Instant expiresAt)
            implements Decision {
        }
    }

    Decision checkBeforeCall(ToolCall toolCall);

    void recordAfterCall(ToolCall toolCall, @Nullable Object output);

    void recordApprovalResolution(
        List<Long> ruleIds, ToolCall toolCall, boolean approved, @Nullable String approvedBy);
}
