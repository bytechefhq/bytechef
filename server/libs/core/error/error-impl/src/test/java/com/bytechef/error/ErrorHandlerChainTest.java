
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

package com.bytechef.error;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class ErrorHandlerChainTest {

    @Test
    public void test1() {
        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        ErrorHandlerChain chain = new ErrorHandlerChain((List) List.of(new JobErrorHandler()));

        chain.handle(new Job());
    }

    @Test
    public void test2() {

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        ErrorHandlerChain chain = new ErrorHandlerChain(
            (List) Arrays.asList(new Test1ErrorHandler(), new Test2ErrorHandler()));

        chain.handle(new TaskExecution());
    }

    private static class Job implements Errorable {

        @Override
        public ExecutionError getError() {
            return null;
        }
    }

    private static class JobErrorHandler implements ErrorHandler<Job> {

        @Override
        public void handle(Job job) {
            Assertions.assertEquals(Job.class, job.getClass());
        }

        @Override
        public Class<?> getType() {
            return JobErrorHandler.class;
        }
    }

    private static class TaskExecution implements Errorable {

        @Override
        public ExecutionError getError() {
            return null;
        }
    }

    private static class Test1ErrorHandler implements ErrorHandler<ErrorHandlerChainTest.Job> {

        @Override
        public void handle(Job j) {
            throw new IllegalStateException("should not get here");
        }

        @Override
        public Class<?> getType() {
            return Test1ErrorHandler.class;
        }
    }

    private static class Test2ErrorHandler implements ErrorHandler<ErrorHandlerChainTest.TaskExecution> {

        @Override
        public void handle(ErrorHandlerChainTest.TaskExecution jt) {
            Assertions.assertEquals(ErrorHandlerChainTest.TaskExecution.class, jt.getClass());
        }

        @Override
        public Class<?> getType() {
            return Test2ErrorHandler.class;
        }
    }
}
