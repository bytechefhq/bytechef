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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;

/**
 * @author Ivica Cardic
 */
public interface PrincipalJobFacade {

    long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type);

    Job createSyncJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type);
}
