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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ExecutionDataJobCompletionListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ExecutionDataJobCompletionListener.class);

    private final @Nullable DataStorage dataStorage;
    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    public ExecutionDataJobCompletionListener(@Nullable DataStorage dataStorage, JobService jobService) {
        this.dataStorage = dataStorage;
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (dataStorage == null ||
            !(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) ||
            jobStatusApplicationEvent.getStatus() != Job.Status.COMPLETED) {

            return;
        }

        Job job = jobService.getJob(jobStatusApplicationEvent.getJobId());

        if (job.getParentTaskExecutionId() != null ||
            MapUtils.getBoolean(job.getMetadata(), MetadataConstants.EDITOR_ENVIRONMENT, false)) {

            return;
        }

        List<Long> completedJobIds = new ArrayList<>();

        completedJobIds.add(jobStatusApplicationEvent.getJobId());

        addCompletedChildJobIds(jobStatusApplicationEvent.getJobId(), completedJobIds);

        for (long completedJobId : completedJobIds) {
            deleteCurrentExecutionData(dataStorage, completedJobId);
        }
    }

    private void addCompletedChildJobIds(long jobId, List<Long> completedJobIds) {
        for (long childJobId : jobService.getChildJobIds(jobId)) {
            Job childJob = jobService.getJob(childJobId);

            if (childJob.getStatus() == Job.Status.COMPLETED) {
                completedJobIds.add(childJobId);
            }

            addCompletedChildJobIds(childJobId, completedJobIds);
        }
    }

    private static void deleteCurrentExecutionData(DataStorage dataStorage, long jobId) {
        String scopeId = String.valueOf(jobId);

        for (PlatformType type : PlatformType.values()) {
            try {
                dataStorage.deleteScopeData(DataStorageScope.CURRENT_EXECUTION, scopeId, type);
            } catch (UnsupportedOperationException exception) {
                log.debug("Scope-wide Data Storage delete is unsupported by this provider; skipping job {}", jobId);

                return;
            } catch (RuntimeException exception) {
                log.warn("Failed to delete the Current Execution Data Storage values of job {}", jobId, exception);
            }
        }
    }
}
