
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

package com.bytechef.atlas.event;

import java.time.LocalDateTime;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 8, 2017
 */
public abstract class WorkflowEvent {

    private final LocalDateTime createdDate;
    protected String type;

    protected WorkflowEvent() {
        this.createdDate = LocalDateTime.now();
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "WorkflowEvent{" + "createTime=" + createdDate + ", type='" + type + '\'' + '}';
    }
}
