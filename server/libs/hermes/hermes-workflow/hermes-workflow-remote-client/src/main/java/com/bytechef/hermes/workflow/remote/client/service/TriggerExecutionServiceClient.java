
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

package com.bytechef.hermes.workflow.remote.client.service;

import com.bytechef.hermes.workflow.domain.TriggerExecution;
import com.bytechef.hermes.workflow.service.TriggerExecutionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionServiceClient implements TriggerExecutionService {

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return null;
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return null;
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return null;
    }
}
