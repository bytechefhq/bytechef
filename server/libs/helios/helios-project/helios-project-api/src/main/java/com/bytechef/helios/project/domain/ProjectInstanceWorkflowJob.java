
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.project.domain;

import com.bytechef.tag.domain.Tag;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("project_instance_workflow_job")
public final class ProjectInstanceWorkflowJob implements Persistable<Long> {

    @Id
    private Long id;

    @Column("job_id")
    private AggregateReference<Tag, Long> jobId;

    @Column("project_instance_workflow_id")
    private AggregateReference<Tag, Long> projectInstanceWorkflowId;

    public ProjectInstanceWorkflowJob() {
    }

    public ProjectInstanceWorkflowJob(long projectInstanceWorkflowId, long jobId) {
        this.projectInstanceWorkflowId = AggregateReference.to(projectInstanceWorkflowId);
        this.jobId = AggregateReference.to(jobId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectInstanceWorkflowJob that = (ProjectInstanceWorkflowJob) o;

        return Objects.equals(id, that.id) && Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId.getId();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String toString() {
        return "ProjectInstanceJob{" +
            "id=" + id +
            ", jobId=" + jobId +
            ", projectInstanceWorkflowId=" + projectInstanceWorkflowId +
            '}';
    }
}
