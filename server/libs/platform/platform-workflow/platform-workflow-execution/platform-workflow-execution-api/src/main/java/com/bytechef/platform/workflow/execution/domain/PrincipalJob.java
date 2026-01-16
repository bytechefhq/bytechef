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

package com.bytechef.platform.workflow.execution.domain;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.constant.PlatformType;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("principal_job")
public class PrincipalJob {

    @Id
    private Long id;

    @Column("principal_id")
    private Long principalId;

    @Column("job_id")
    private AggregateReference<Job, Long> jobId;

    @Column
    private int type;

    public PrincipalJob() {
    }

    public PrincipalJob(long principalId, long jobId, PlatformType type) {
        this.jobId = AggregateReference.to(jobId);
        this.principalId = principalId;
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrincipalJob that = (PrincipalJob) o;

        return Objects.equals(id, that.id) && Objects.equals(principalId, that.principalId) &&
            Objects.equals(jobId, that.jobId) && type == that.type;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getPrincipalId() {
        return principalId;
    }

    public Long getJobId() {
        return jobId.getId();
    }

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    @Override
    public String toString() {
        return "JobInstance{" +
            "id=" + id +
            ", principalId=" + principalId +
            ", jobId=" + jobId +
            ", type=" + type +
            '}';
    }
}
