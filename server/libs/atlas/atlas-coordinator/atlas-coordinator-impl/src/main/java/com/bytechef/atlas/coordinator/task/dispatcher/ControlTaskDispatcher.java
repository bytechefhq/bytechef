/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.ControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Arik Cohen
 * @since Apr 11, 2017
 */
public class ControlTaskDispatcher implements TaskDispatcher<ControlTask>, TaskDispatcherResolver {

    private final ApplicationEventPublisher eventPublisher;

    public ControlTaskDispatcher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void dispatch(ControlTask controlTask) {
        if (controlTask instanceof CancelControlTask cancelControlTask) {
            eventPublisher.publishEvent(new CancelControlTaskEvent(cancelControlTask));
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof ControlTask) {
            return this;
        }

        return null;
    }
}
