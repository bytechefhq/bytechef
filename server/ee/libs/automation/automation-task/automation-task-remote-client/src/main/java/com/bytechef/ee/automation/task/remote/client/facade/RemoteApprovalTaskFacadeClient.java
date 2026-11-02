/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.client.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteApprovalTaskFacadeClient implements ApprovalTaskFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String APPROVAL_TASK_FACADE = "/remote/approval-task-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteApprovalTaskFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(APPROVAL_TASK_FACADE + "/create-approval-task")
                .build(),
            approvalTask, ApprovalTask.class);
    }

    @Override
    public List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId) {
        return loadBalancedRestClient.get(
            uriBuilder -> {
                uriBuilder.host(CONFIGURATION_APP)
                    .path(APPROVAL_TASK_FACADE + "/get-pending-approvals");

                if (environmentId != null) {
                    uriBuilder.queryParam("environmentId", environmentId);
                }

                return uriBuilder.build();
            },
            new ParameterizedTypeReference<>() {});
    }
}
