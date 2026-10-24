/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.definition.ai.agent;

import java.util.List;
import java.util.Map;

/**
 * What a task execution recorded: the per-element aggregates and every individual call in call order.
 *
 * @author Ivica Cardic
 */
public record ClusterElementCallDrain(Map<String, ClusterElementCall> calls, List<ClusterElementCallRecord> records) {

    public ClusterElementCallDrain {
        calls = Map.copyOf(calls);
        records = List.copyOf(records);
    }
}
