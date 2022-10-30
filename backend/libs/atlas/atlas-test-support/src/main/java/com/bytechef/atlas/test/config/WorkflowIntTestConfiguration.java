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

package com.bytechef.atlas.test.config;

import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.coordinator.event.EventListenerChain;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.test.workflow.WorkflowExecutor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class WorkflowIntTestConfiguration implements InitializingBean {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired(required = false)
    List<TaskHandlerRegistrar> taskHandlerRegistrars = List.of();

    public void afterPropertiesSet() {
        //        ObjectMap.addConverter(new ErrorConverter());
        //        ObjectMap.addConverter(new WorkflowTaskConverter());

        for (TaskHandlerRegistrar taskHandlerRegistrar : taskHandlerRegistrars) {
            taskHandlerRegistrar.registerTaskHandlers(beanFactory);
        }
    }

    @Bean
    WorkflowExecutor workflowTester(
            ContextService contextService,
            CounterService counterService,
            JobService jobService,
            EventPublisher eventPublisher,
            TaskExecutionService taskExecutionService,
            @Autowired(required = false) Map<String, TaskHandler<?>> taskHandlerMap,
            WorkflowService workflowService) {
        return new WorkflowExecutor(
                contextService,
                counterService,
                jobService,
                eventPublisher,
                taskExecutionService,
                taskHandlerMap == null ? Map.of() : taskHandlerMap,
                workflowService);
    }

    @TestConfiguration
    public static class EventConfiguration {

        @Bean
        @Primary
        EventListenerChain eventListener(List<EventListener> eventListeners) {
            return new EventListenerChain(eventListeners);
        }

        @Bean
        EventPublisher eventPublisher(EventListener eventListener) {
            return eventListener::onApplicationEvent;
        }
    }

    @TestConfiguration
    public static class MessageBrokerConfiguration {

        @Bean
        MessageBroker messageBroker() {
            return new SyncMessageBroker();
        }
    }
}
