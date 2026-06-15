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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Utility methods for finalizing a suspend request after a streaming action completes. Used by post-output processors
 * (Task 12) to replicate the resume-URL / {@code JobResumeId} enrichment that
 * {@code ActionDefinitionServiceImpl.checkSuspend} performs for non-streaming actions.
 *
 * <p>
 * Note: {@code BeforeSuspendConsumer} invocation is intentionally omitted here — it requires the
 * {@code ActionDefinition} and an error-type token from the service layer. AI agent actions (the only streaming suspend
 * callers) do not declare a {@code BeforeSuspendConsumer}, so this omission is acceptable for the streaming path.
 * </p>
 *
 * @author Ivica Cardic
 */
public class SuspendUtils {

    private SuspendUtils() {
    }

    /**
     * Reads {@code actionContextAware.getSuspend()} and, if non-null, enriches the {@code continueParameters} map with
     * the {@code jobResumeId} (when one was generated) and returns the final {@link ActionContext.Suspend}. Returns
     * {@code null} when no suspension was requested.
     *
     * @param actionContextAware the action context to inspect
     * @return the finalized {@link ActionContext.Suspend}, or {@code null} if the action did not suspend
     */
    public static ActionContext.@Nullable Suspend finalizeSuspend(ActionContextAware actionContextAware) {
        ActionContext.Suspend suspend = actionContextAware.getSuspend();

        if (suspend == null) {
            return null;
        }

        String jobResumeId = actionContextAware.getJobResumeId();

        if (jobResumeId != null) {
            Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

            continueParameters.put(MetadataConstants.JOB_RESUME_ID, jobResumeId);

            return new ActionContext.Suspend(continueParameters, suspend.expiresAt());
        }

        return suspend;
    }
}
