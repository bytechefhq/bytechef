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

package com.bytechef.platform.workflow.coordinator.message;

import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.event.MessageEventPostReceiveProcessor;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs metrics about received task execution outputs that were filtered to include only referenced property paths.
 *
 * @author Ivica Cardic
 */
@Component
class OutputFilteringMessageEventPostReceiveProcessor implements MessageEventPostReceiveProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutputFilteringMessageEventPostReceiveProcessor.class);

    @Override
    public MessageEvent<?> process(MessageEvent<?> messageEvent) {
        if (!(messageEvent instanceof TaskExecutionCompleteEvent taskExecutionCompleteEvent)) {
            return messageEvent;
        }

        TaskExecution taskExecution = taskExecutionCompleteEvent.getTaskExecution();

        Map<String, ?> metadata = taskExecution.getMetadata();

        Object referencePaths = metadata.get(MetadataConstants.OUTPUT_REFERENCE_PATHS);

        if (referencePaths instanceof Collection<?> referencePathCollection && !referencePathCollection.isEmpty()
            && logger.isDebugEnabled()) {

            logger.debug(
                "Received filtered output for task name='{}', type='{}' with {} referenced paths",
                taskExecution.getName(), taskExecution.getType(), referencePathCollection.size());
        }

        return messageEvent;
    }
}
