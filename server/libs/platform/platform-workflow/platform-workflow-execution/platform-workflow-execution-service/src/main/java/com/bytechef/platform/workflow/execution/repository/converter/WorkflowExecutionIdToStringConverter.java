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

package com.bytechef.platform.workflow.execution.repository.converter;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * @author Ivica Cardic
 */
@WritingConverter
public class WorkflowExecutionIdToStringConverter implements Converter<WorkflowExecutionId, String> {

    @Override
    public String convert(WorkflowExecutionId workflowExecutionId) {
        return workflowExecutionId.toString();
    }
}
