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

package com.bytechef.task.dispatcher.subflow.event.listener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The result of a sub-workflow run, handed back to the agent tool call that launched it.
 *
 * @author Ivica Cardic
 */
sealed interface SubflowOutcome {

    String STATUS = "status";
    String STATUS_COMPLETED = "completed";
    String STATUS_FAILED = "failed";

    Map<String, Object> toResumeData();

    record Completed(@Nullable Object result) implements SubflowOutcome {

        @Override
        public Map<String, Object> toResumeData() {
            Map<String, Object> resumeData = new LinkedHashMap<>();

            resumeData.put(STATUS, STATUS_COMPLETED);

            if (result != null) {
                resumeData.put("result", result);
            }

            return resumeData;
        }
    }

    record Failed(String error) implements SubflowOutcome {

        public Failed {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public Map<String, Object> toResumeData() {
            Map<String, Object> resumeData = new LinkedHashMap<>();

            resumeData.put(STATUS, STATUS_FAILED);
            resumeData.put("error", error);

            return resumeData;
        }
    }
}
