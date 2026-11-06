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

package com.bytechef.platform.scheduler.db.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/**
 * @author Ivica Cardic
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@ComponentScan
@Import(LiquibaseConfiguration.class)
public class DbSchedulerTestConfiguration {

    public static class CapturedEvents {

        public final List<TriggerListenerEvent> listenerEvents = new CopyOnWriteArrayList<>();
        public final List<TriggerPollEvent> pollEvents = new CopyOnWriteArrayList<>();
        public final List<ResumeJobEvent> resumeEvents = new CopyOnWriteArrayList<>();

        @EventListener
        public void onListener(TriggerListenerEvent event) {
            listenerEvents.add(event);
        }

        @EventListener
        public void onPoll(TriggerPollEvent event) {
            pollEvents.add(event);
        }

        @EventListener
        public void onResume(ResumeJobEvent event) {
            resumeEvents.add(event);
        }
    }

    @Bean
    CapturedEvents capturedEvents() {
        return new CapturedEvents();
    }

    @Bean
    ConnectionFacade connectionFacade() {
        return Mockito.mock(ConnectionFacade.class);
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry() {
        return Mockito.mock(JobPrincipalAccessorRegistry.class);
    }

    @Bean
    TriggerDefinitionFacade triggerDefinitionFacade() {
        return Mockito.mock(TriggerDefinitionFacade.class);
    }

    @Bean
    TriggerStateService triggerStateService() {
        return Mockito.mock(TriggerStateService.class);
    }

    @Bean
    WorkflowService workflowService() {
        return Mockito.mock(WorkflowService.class);
    }
}
