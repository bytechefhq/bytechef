/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.job;

import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.error.Error;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Feb, 25 2020
 */
public class JobSummary {

    private final Job job;

    public JobSummary(Job aJob) {
        job = Objects.requireNonNull(aJob);
    }

    public int getPriority() {
        return job.getPriority();
    }

    public Error getError() {
        return job.getError();
    }

    public String getId() {
        return job.getId();
    }

    public String getParentTaskExecutionId() {
        return job.getParentTaskExecutionId();
    }

    public JobStatus getStatus() {
        return job.getStatus();
    }

    public int getCurrentTask() {
        return job.getCurrentTask();
    }

    public String getWorkflowId() {
        return job.getWorkflowId();
    }

    public String getLabel() {
        return job.getLabel();
    }

    public Date getCreateTime() {
        return job.getCreateTime();
    }

    public Date getStartTime() {
        return job.getStartTime();
    }

    public Date getEndTime() {
        return job.getEndTime();
    }

    public Accessor getInputs() {
        return job.getInputs();
    }

    public Accessor getOutputs() {
        return job.getOutputs();
    }

    public List<Accessor> getWebhooks() {
        return job.getWebhooks();
    }
}
