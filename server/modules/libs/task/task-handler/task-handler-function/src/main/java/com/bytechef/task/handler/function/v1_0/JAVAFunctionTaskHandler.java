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

package com.bytechef.task.handler.function.v1_0;

import static com.bytechef.task.handler.function.FunctionTaskConstants.*;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.handler.function.PolyglotEngine;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component(FUNCTION + "/" + VERSION_1_0 + "/" + JAVA)
public class JAVAFunctionTaskHandler implements TaskHandler<Object> {

    private final PolyglotEngine polyglotEngine;

    public JAVAFunctionTaskHandler(PolyglotEngine polyglotEngine) {
        this.polyglotEngine = polyglotEngine;
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        return polyglotEngine.execute(JAVA, taskExecution);
    }
}
