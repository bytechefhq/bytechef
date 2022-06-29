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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.context.repository.ContextRepository;
import com.bytechef.atlas.context.service.ContextService;
import com.bytechef.atlas.coordinator.util.TestConfigurator;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.service.JobService;
import com.bytechef.atlas.workflow.repository.mapper.JSONWorkflowMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class DeleteContextEventListenerIntTest {

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private TestConfigurator testConfigurator;

    @Test
    public void testJobStatusEventHandler() {
        Job job = testConfigurator.startJob("samples/hello.json", new JSONWorkflowMapper());

        Assertions.assertNull(contextRepository.peek(job.getId()));
    }

    @TestConfiguration
    public static class DeleteContextEventListenerIntTestConfiguration {

        @Bean
        DeleteContextEventListener deleteContextEventListener(ContextService contextService, JobService jobService) {
            return new DeleteContextEventListener(contextService, jobService);
        }
    }
}
