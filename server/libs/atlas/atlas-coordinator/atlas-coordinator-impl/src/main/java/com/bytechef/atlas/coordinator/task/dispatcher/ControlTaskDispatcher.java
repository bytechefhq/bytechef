
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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.task.ControlTask;
import com.bytechef.message.broker.ExchangeType;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.task.Task;

import java.util.Objects;

/**
 * @author Arik Cohen
 * @since Apr 11, 2017
 */
public class ControlTaskDispatcher implements TaskDispatcher<ControlTask>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;

    public ControlTaskDispatcher(MessageBroker messageBroker) {
        this.messageBroker = Objects.requireNonNull(messageBroker);
    }

    @Override
    public void dispatch(ControlTask controlTask) {
        messageBroker.send(ExchangeType.CONTROL + "/" + ExchangeType.CONTROL, controlTask);
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof ControlTask) {
            return this;
        }

        return null;
    }
}
