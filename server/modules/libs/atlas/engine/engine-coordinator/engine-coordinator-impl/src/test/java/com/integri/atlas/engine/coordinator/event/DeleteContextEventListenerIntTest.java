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

package com.integri.atlas.engine.coordinator.event;

import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.context.service.ContextService;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.service.JobService;
import com.integri.atlas.test.task.handler.BaseTaskIntTest;
import java.util.Map;
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
public class DeleteContextEventListenerIntTest extends BaseTaskIntTest {

    @Autowired
    private ContextRepository contextRepository;

    @Test
    public void testJobStatusEventHandler() {
        Job job = startJob("samples/hello.json", Map.of("yourName", "me"));

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
