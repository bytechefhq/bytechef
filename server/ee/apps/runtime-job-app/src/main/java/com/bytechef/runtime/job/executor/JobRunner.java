/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.executor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class JobRunner {

    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public JobRunner(JobFacade jobFacade, JobService jobService, TaskFileStorage taskFileStorage) {
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.taskFileStorage = taskFileStorage;
    }

    public Map<String, ?> getOutputs(long jobId) {
        Job job = jobService.getJob(jobId);

        FileEntry outputs = job.getOutputs();

        if (outputs == null) {
            return Map.of();
        }

        return taskFileStorage.readJobOutputs(outputs);
    }

    public long run(String workflowName, Map<String, ?> jobParameters) {
        String substring = workflowName.substring(workflowName.lastIndexOf("/") + 1, workflowName.lastIndexOf('.'));

        String id = EncodingUtils.base64EncodeToString(substring);

        return jobFacade.createJob(new JobParametersDTO(id, jobParameters));
    }
}
