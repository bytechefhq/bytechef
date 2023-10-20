
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

package com.bytechef.hermes.worker.trigger.handler;

import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;

/**
 * A strategy interface used for executing a {@link TriggerExecution}.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public interface TriggerHandler {

    TriggerDefinition.TriggerOutput handle(TriggerExecution triggerExecution) throws TriggerExecutionException;
}
