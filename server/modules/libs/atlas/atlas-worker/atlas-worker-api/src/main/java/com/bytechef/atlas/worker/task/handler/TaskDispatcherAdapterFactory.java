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

package com.bytechef.atlas.worker.task.handler;

import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;

/**
 * @author Ivica Cardic
 */
public interface TaskDispatcherAdapterFactory {

    TaskHandler<?> create(TaskHandlerResolver taskHandlerResolver, TaskEvaluator taskEvaluator, Worker.Builder builder);

    String getName();
}
