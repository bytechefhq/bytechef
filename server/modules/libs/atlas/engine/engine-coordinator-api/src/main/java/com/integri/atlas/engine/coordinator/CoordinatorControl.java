/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.coordinator;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.core.error.Errorable;
import com.integri.atlas.engine.core.task.TaskExecution;
import java.util.Map;

public interface CoordinatorControl {
    void complete(TaskExecution completion);

    Job create(Map<String, Object> aJobParams);

    void handleError(Errorable errorable);

    Job resume(String aJobId);

    Job stop(String jobId);
}
