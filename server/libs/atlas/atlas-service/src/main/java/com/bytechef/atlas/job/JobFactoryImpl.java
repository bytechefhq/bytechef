
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

package com.bytechef.atlas.job;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

public class JobFactoryImpl implements JobFactory {

    private final ContextService contextService;
    private final EventPublisher eventPublisher;
    private final JobService jobService;
    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI2")
    public JobFactoryImpl(
        ContextService contextService, EventPublisher eventPublisher, JobService jobService,
        MessageBroker messageBroker) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
    }

    @Override
    @SuppressFBWarnings("NP")
    public long create(JobParametersDTO jobParametersDTO) {
        Job job = jobService.create(jobParametersDTO);

        Assert.notNull(job.getId(), "'job.id' must not be null");

        contextService.push(job.getId(), Context.Classname.JOB, job.getInputs());

        eventPublisher.publishEvent(new JobStatusWorkflowEvent(job.getId(), job.getStatus()));

        messageBroker.send(Queues.JOBS, job.getId());

        return job.getId();
    }
}
