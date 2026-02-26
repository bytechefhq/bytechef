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

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ChildJobPrincipalFactoryImpl implements ChildJobPrincipalFactory {

    private final PrincipalJobFacade principalJobFacade;

    ChildJobPrincipalFactoryImpl(PrincipalJobFacade principalJobFacade) {
        this.principalJobFacade = principalJobFacade;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
        return principalJobFacade.createChildJob(parentJobId, jobParametersDTO, PlatformType.AUTOMATION);
    }
}
