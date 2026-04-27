/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.client.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteApprovalTaskFacadeClient implements ApprovalTaskFacade {

    @Override
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        throw new UnsupportedOperationException();
    }
}
