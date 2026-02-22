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

package com.bytechef.automation.configuration.subflow;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalCreator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ChildJobPrincipalCreatorImpl implements ChildJobPrincipalCreator {

    private static final Logger logger = LoggerFactory.getLogger(ChildJobPrincipalCreatorImpl.class);

    private final PrincipalJobService principalJobService;

    ChildJobPrincipalCreatorImpl(PrincipalJobService principalJobService) {
        this.principalJobService = principalJobService;
    }

    @Override
    public void createPrincipalForChildJob(long parentJobId, long childJobId) {
        Optional<Long> principalId = principalJobService.fetchJobPrincipalId(parentJobId, PlatformType.AUTOMATION);

        if (principalId.isPresent()) {
            principalJobService.create(childJobId, principalId.get(), PlatformType.AUTOMATION);
        } else {
            logger.warn(
                "No principal found for parent job {} -- child job {} will have no principal association",
                parentJobId, childJobId);
        }
    }
}
