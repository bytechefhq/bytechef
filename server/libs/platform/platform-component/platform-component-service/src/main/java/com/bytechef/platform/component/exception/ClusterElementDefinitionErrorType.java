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

package com.bytechef.platform.component.exception;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ClusterElementDefinitionErrorType extends AbstractErrorType {

    public static final ClusterElementDefinitionErrorType EXECUTE_DYNAMIC_PROPERTIES =
        new ClusterElementDefinitionErrorType(100);
    public static final ClusterElementDefinitionErrorType EXECUTE_WORKFLOW_NODE_DESCRIPTION =
        new ClusterElementDefinitionErrorType(101);
    public static final ClusterElementDefinitionErrorType EXECUTE_OPTIONS = new ClusterElementDefinitionErrorType(102);
    public static final ClusterElementDefinitionErrorType EXECUTE_PERFORM = new ClusterElementDefinitionErrorType(104);
    public static final ClusterElementDefinitionErrorType EXECUTE_PROCESS_ERROR_RESPONSE =
        new ClusterElementDefinitionErrorType(105);

    private ClusterElementDefinitionErrorType(int errorKey) {
        super(ClusterElementDefinition.class, errorKey);
    }
}
