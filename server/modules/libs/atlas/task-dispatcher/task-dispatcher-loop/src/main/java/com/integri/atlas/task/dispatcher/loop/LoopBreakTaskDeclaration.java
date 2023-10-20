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

package com.integri.atlas.task.dispatcher.loop;

import static com.integri.atlas.task.dispatcher.loop.LoopBreakTaskConstants.TASK_LOOP_BREAK;
import static com.integri.atlas.task.dispatcher.loop.LoopTaskConstants.TASK_LOOP;

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;

/**
 * @author Ivica Cardic
 */
public class LoopBreakTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_LOOP_BREAK)
        .displayName("Loop Break")
        .description("Breaks loop execution.");

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
