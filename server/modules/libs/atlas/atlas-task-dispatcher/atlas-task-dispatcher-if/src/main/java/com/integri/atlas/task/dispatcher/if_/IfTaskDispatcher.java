/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.dispatcher.if_;

import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.Task;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.repository.CounterRepository;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;

/**
 * @author Ivica Cardic
 */
public class IfTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private ContextRepository contextRepository;
    private CounterRepository counterRepository;
    private MessageBroker messageBroker;
    private TaskDispatcher taskDispatcher;
    private TaskExecutionRepository taskExecutionRepo;

    @Override
    public void dispatch(TaskExecution aTask) {

    }

    public void setCounterRepository(CounterRepository aCounterRepository) {
        counterRepository = aCounterRepository;
    }

    public void setContextRepository(ContextRepository aContextRepository) {
        contextRepository = aContextRepository;
    }

    public void setMessageBroker(MessageBroker aMessageBroker) {
        messageBroker = aMessageBroker;
    }

    public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        taskDispatcher = aTaskDispatcher;
    }

    public void setTaskExecutionRepository(TaskExecutionRepository aTaskExecutionRepo) {
        taskExecutionRepo = aTaskExecutionRepo;
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType().equals(DSL.IF)) {
            return this;
        }
        return null;
    }
}
