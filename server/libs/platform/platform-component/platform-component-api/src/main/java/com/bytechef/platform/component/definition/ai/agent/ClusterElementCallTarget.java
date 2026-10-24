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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;

/**
 * The recorder, job and task execution a cluster element reports its calls and outcomes against.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public record ClusterElementCallTarget(ClusterElementCallRecorder recorder, long jobId, long taskExecutionId) {

    public static Optional<ClusterElementCallTarget> of(Context context) {
        if (context instanceof ActionContextAware actionContextAware) {
            Long jobId = actionContextAware.getJobId();
            Long taskExecutionId = actionContextAware.getTaskExecutionId();

            if (jobId != null && taskExecutionId != null) {
                return Optional.of(
                    new ClusterElementCallTarget(ClusterElementCallRecorder.getInstance(), jobId, taskExecutionId));
            }
        }

        return Optional.empty();
    }
}
