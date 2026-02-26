/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.task.dispatcher.subflow;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalCreator;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class RuntimeJobChildJobPrincipalCreator implements ChildJobPrincipalCreator {

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {
        throw new UnsupportedOperationException();
    }
}
