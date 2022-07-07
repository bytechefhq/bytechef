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

package com.bytechef.task.dispatcher.subflow.configuration;

import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class SubflowTaskDispatcherConfiguration {

    @Autowired
    private MessageBroker messageBroker;

    @Bean
    TaskDispatcherResolverFactory subflowTaskDispatcherFactory() {
        return (taskDispatcher) -> new SubflowTaskDispatcher(messageBroker);
    }
}
