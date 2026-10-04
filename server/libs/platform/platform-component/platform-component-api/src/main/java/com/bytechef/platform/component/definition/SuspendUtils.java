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
 * @author Ivica Cardic
 */
public final class SuspendUtils {

    private SuspendUtils() {
    }

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
