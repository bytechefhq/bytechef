/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.domain;

/**
 * @author Ivica Cardic
 */
public class CancelControlTrigger implements ControlTrigger {

    public static final String TYPE_CANCEL = "cancel";

    private long triggerExecutionId;

    private CancelControlTrigger() {
    }

    public CancelControlTrigger(Long triggerExecutionId) {
        this.triggerExecutionId = triggerExecutionId;
    }

    public long getTriggerExecutionId() {
        return triggerExecutionId;
    }

    @Override
    public String toString() {
        return "CancelControlTrigger{, triggerExecutionId='" + triggerExecutionId + '}';
    }

    @Override
    public String getType() {
        return TYPE_CANCEL;
    }
}
