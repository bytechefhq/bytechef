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

package com.bytechef.component.map.config;

import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.component.map.MapTaskDispatcherAdapterTaskHandler;
import com.bytechef.component.map.constant.MapConstants;
import com.bytechef.evaluator.Evaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class MapTaskDispatcherAdapterConfiguration {

    @Bean
    TaskDispatcherAdapterFactory taskDispatcherAdapterFactory(Evaluator evaluator) {
        return new TaskDispatcherAdapterFactory() {

            @Override
            public TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver) {
                return new MapTaskDispatcherAdapterTaskHandler(evaluator, taskHandlerResolver);
            }

            @Override
            public String getName() {
                return MapConstants.MAP + "/v1";
            }
        };
    }
}
