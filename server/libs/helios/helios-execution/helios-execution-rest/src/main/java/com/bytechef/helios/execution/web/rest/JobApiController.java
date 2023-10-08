
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.helios.execution.web.rest;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.execution.web.rest.model.CreateJob200ResponseModel;
import com.bytechef.helios.execution.web.rest.model.JobBasicModel;
import com.bytechef.helios.execution.web.rest.model.JobModel;
import com.bytechef.helios.execution.web.rest.model.JobParametersModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class JobApiController implements JobApi {

    private final ConversionService conversionService;
    private final JobFacade jobFacade;
    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    public JobApiController(ConversionService conversionService, JobFacade jobFacade, JobService jobService) {
        this.conversionService = conversionService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
    }

    @Override
    public ResponseEntity<CreateJob200ResponseModel> createJob(JobParametersModel jobParametersModel) {
        return ResponseEntity.ok(
            new CreateJob200ResponseModel()
                .jobId(jobFacade.createJob(conversionService.convert(jobParametersModel, JobParameters.class))));
    }

    @Override
    public ResponseEntity<JobModel> getJob(Long id) {
        return ResponseEntity.ok(conversionService.convert(jobService.getJob(id), JobModel.class));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ResponseEntity<Page> getJobs(Integer pageNumber) {
        return ResponseEntity.ok(
            jobService.getJobs(pageNumber)
                .map(job -> conversionService.convert(job, JobBasicModel.class)));
    }

    @Override
    public ResponseEntity<JobModel> getLatestJob() {
        return ResponseEntity.ok(
            conversionService.convert(OptionalUtils.orElse(jobService.fetchLatestJob(), null), JobModel.class));
    }

    @Override
    public ResponseEntity<Void> restartJob(Long id) {
        jobFacade.restartJob(id);

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
