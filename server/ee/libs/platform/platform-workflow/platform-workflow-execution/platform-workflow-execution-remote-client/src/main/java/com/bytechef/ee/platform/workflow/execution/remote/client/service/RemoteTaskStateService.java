/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskStateService implements TaskStateService {

    @Override
    public void delete(JobResumeId jobResumeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(JobResumeId jobResumeId, Object value) {
        throw new UnsupportedOperationException();
    }
}
