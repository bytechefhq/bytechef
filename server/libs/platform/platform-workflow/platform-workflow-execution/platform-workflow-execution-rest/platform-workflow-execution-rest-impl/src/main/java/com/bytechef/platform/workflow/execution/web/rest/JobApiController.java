/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.platform.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.workflow.execution.web.rest.model.JobBasicModel;
import com.bytechef.platform.workflow.execution.web.rest.model.JobModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class JobApiController implements JobApi {

    private final ConversionService conversionService;
    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI2")
    public JobApiController(ConversionService conversionService, JobFacade jobFacade) {
        this.conversionService = conversionService;
        this.jobFacade = jobFacade;
    }

    @Override
    public ResponseEntity<JobModel> getJob(Long id) {
        return ResponseEntity.ok(conversionService.convert(jobFacade.getJob(id), JobModel.class));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ResponseEntity<Page> getJobsPage(Integer pageNumber) {
        return ResponseEntity.ok(
            jobFacade.getJobsPage(pageNumber)
                .map(job -> conversionService.convert(job, JobBasicModel.class)));
    }

    @Override
    public ResponseEntity<JobModel> getLatestJob() {
        return ResponseEntity.ok(conversionService.convert(jobFacade.getLatestJob(), JobModel.class));
    }

    @Override
    public ResponseEntity<Void> restartJob(Long id) {
        jobFacade.resumeJob(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> stopJob(Long id) {
        jobFacade.stopJob(id);

        return ResponseEntity.noContent()
            .build();
    }
}
