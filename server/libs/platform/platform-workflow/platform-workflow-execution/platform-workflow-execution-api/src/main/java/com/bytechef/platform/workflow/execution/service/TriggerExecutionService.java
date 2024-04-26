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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface TriggerExecutionService {

    TriggerExecution create(TriggerExecution triggerExecution);

    void delete(long id);

    void deleteJobTriggerExecution(long id);

    Optional<TriggerExecution> fetchJobTriggerExecution(long jobId);

    TriggerExecution getTriggerExecution(long id);

    TriggerExecution update(TriggerExecution triggerExecution);
}
