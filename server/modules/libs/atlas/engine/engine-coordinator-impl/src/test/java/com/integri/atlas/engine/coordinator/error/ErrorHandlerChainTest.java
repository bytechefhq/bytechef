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

package com.integri.atlas.engine.coordinator.error;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.SimpleJob;
import com.integri.atlas.engine.core.error.ErrorHandler;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class ErrorHandlerChainTest {

    @Test
    public void test1() {
        ErrorHandler errorHandler = new ErrorHandler<Job>() {
            @Override
            public void handle(Job j) {
                Assertions.assertEquals(SimpleJob.class, j.getClass());
            }
        };
        ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler));
        chain.handle(new SimpleJob());
    }

    @Test
    public void test2() {
        ErrorHandler errorHandler1 = new ErrorHandler<Job>() {
            @Override
            public void handle(Job j) {
                throw new IllegalStateException("should not get here");
            }
        };
        ErrorHandler errorHandler2 = new ErrorHandler<TaskExecution>() {
            @Override
            public void handle(TaskExecution jt) {
                Assertions.assertEquals(SimpleTaskExecution.class, jt.getClass());
            }
        };
        ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler1, errorHandler2));
        chain.handle(new SimpleTaskExecution());
    }
}
