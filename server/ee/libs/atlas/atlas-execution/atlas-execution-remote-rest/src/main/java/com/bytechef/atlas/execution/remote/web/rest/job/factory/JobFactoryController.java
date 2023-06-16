
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.execution.remote.web.rest.job.factory;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.job.factory.JobFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class JobFactoryController {

    private final JobFactory jobFactory;

    @SuppressFBWarnings("EI")
    public JobFactoryController(JobFactory jobFactory) {
        this.jobFactory = jobFactory;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/job-factory/create-job")
    public ResponseEntity<Long> create(@Valid @RequestBody JobParameters jobParameters) {
        return ResponseEntity.ok(jobFactory.createJob(jobParameters));
    }
}
