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

package com.bytechef.platform.workflow.validator.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;

/**
 * Domain error codes for workflow validation. Keys are numeric and MUST remain stable — downstream consumers (clients,
 * exception resolvers, test assertions) key off {@link #getErrorKey()}.
 *
 * @author Ivica Cardic
 */
public class WorkflowValidatorErrorType extends AbstractErrorType {

    public static final WorkflowValidatorErrorType DUPLICATE_NODE_NAMES = new WorkflowValidatorErrorType(100);

    public WorkflowValidatorErrorType(int errorKey) {
        super(WorkflowValidatorFacade.class, errorKey);
    }
}
