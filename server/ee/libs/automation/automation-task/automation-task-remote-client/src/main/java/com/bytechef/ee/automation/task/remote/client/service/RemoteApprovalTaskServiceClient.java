/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.client.service;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteApprovalTaskServiceClient implements ApprovalTaskService {

    @Override
    public long countApprovalTasks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApprovalTask create(ApprovalTask approvalTask) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ApprovalTask> fetchApprovalTask(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApprovalTask getApprovalTask(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApprovalTask> getApprovalTasks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApprovalTask> getApprovalTasks(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApprovalTask update(ApprovalTask approvalTask) {
        throw new UnsupportedOperationException();
    }
}
