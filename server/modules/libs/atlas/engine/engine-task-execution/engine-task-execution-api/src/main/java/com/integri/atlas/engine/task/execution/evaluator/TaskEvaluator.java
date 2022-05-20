/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.task.execution.evaluator;

import com.integri.atlas.engine.context.Context;
import com.integri.atlas.engine.task.execution.TaskExecution;

/**
 * Strategy interface for evaluating a TaskExecution.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public interface TaskEvaluator {
    /**
     * Evaluate the {@link TaskExecution}
     *
     * @param taskExecution
     *          The {@link TaskExecution} instance to evaluate
     * @param context
     *          The context to evaluate the task against
     * @return the evaluate {@link TaskExecution}.
     */
    TaskExecution evaluate(TaskExecution taskExecution, Context context);
}
