/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.execution.domain;

import com.bytechef.atlas.execution.domain.Job;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("trigger_execution_job")
public class TriggerExecutionJob {

    @Column("job_id")
    private AggregateReference<Job, Long> jobId;

    public TriggerExecutionJob() {
    }

    public TriggerExecutionJob(Long jobId) {
        this.jobId = jobId == null ? null : AggregateReference.to(jobId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TriggerExecutionJob that)) {
            return false;
        }

        return Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    public Long getJobId() {
        return jobId.getId();
    }

    @Override
    public String toString() {
        return "TriggerExecutionJob{" +
            "jobId=" + jobId +
            '}';
    }
}
