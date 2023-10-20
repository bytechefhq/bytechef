
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

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.worker.task.factory.TaskHandlerMapFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class TaskHandlerRegistryConfiguration {

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(
        Map<String, TaskHandler<?>> taskHandlerMap,
        @Autowired(required = false) TaskHandlerMapFactory taskHandlerMapFactory) {

        return MapUtils.concat(
            taskHandlerMap,
            taskHandlerMapFactory.getTaskHandlerMap() == null
                ? Map.of() : taskHandlerMapFactory.getTaskHandlerMap())::get;
    }
}
