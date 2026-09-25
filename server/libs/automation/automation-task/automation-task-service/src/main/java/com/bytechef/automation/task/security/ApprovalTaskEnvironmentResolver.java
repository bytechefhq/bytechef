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

package com.bytechef.automation.task.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reports the environment of an approval task raised for a job as the environment of the deployment that ran the job,
 * which {@code ApprovalTaskFacade} records on the task when it is raised. A task raised for no job has no environment.
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalTaskEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ApprovalTaskRepository approvalTaskRepository;

    @SuppressFBWarnings("EI")
    public ApprovalTaskEnvironmentResolver(ApprovalTaskRepository approvalTaskRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
    }

    @Override
    public String resourceType() {
        return "ApprovalTask";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return approvalTaskRepository.findById(number.longValue())
            .filter(approvalTask -> approvalTask.getJobResumeId() != null)
            .map(ApprovalTask::getEnvironment);
    }
}
