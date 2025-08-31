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

package com.bytechef.platform.configuration.facade;

import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.domain.OutputResponse;
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeScriptFacadeImpl implements WorkflowNodeScriptFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowNodeScriptFacadeImpl.class);

    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    public WorkflowNodeScriptFacadeImpl(WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade) {
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
    }

    @Override
    public ScriptTestExecutionDTO testWorkflowNodeScript(
        String workflowId, String workflowNodeName, long environmentId) {

        ExecutionError executionError = null;
        WorkflowNodeTestOutput workflowNodeTestOutput = null;

        try {
            workflowNodeTestOutput = workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                workflowId, workflowNodeName, environmentId);
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug(exception.getMessage(), exception);
            }

            Throwable curException = exception;
            String message = null;

            while (curException.getCause() != null) {
                curException = curException.getCause();

                message = curException.getMessage();
            }

            executionError = new ExecutionError(message, Arrays.asList(ExceptionUtils.getStackFrames(exception)));
        }

        OutputResponse outputResponse = null;

        if (workflowNodeTestOutput != null) {
            outputResponse = workflowNodeTestOutput.getOutput(Property.class);
        }

        return new ScriptTestExecutionDTO(
            executionError, outputResponse == null ? null : outputResponse.sampleOutput());
    }
}
