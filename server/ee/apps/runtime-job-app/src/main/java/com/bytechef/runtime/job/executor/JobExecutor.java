/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.executor;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.EncodingUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class JobExecutor {

    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public JobExecutor(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    public void execute(String workflowName, Map<String, ?> jobParameters) {
        String substring = workflowName.substring(workflowName.lastIndexOf("/") + 1, workflowName.lastIndexOf('.'));

        String id = EncodingUtils.base64EncodeToString(substring);

        jobFacade.createJob(new JobParametersDTO(id, jobParameters));
    }
}
