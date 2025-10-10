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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionErrorType extends AbstractErrorType {

    public static final TriggerDefinitionErrorType DYNAMIC_PROPERTIES_FAILED = new TriggerDefinitionErrorType(100);
    public static final TriggerDefinitionErrorType WORKFLOW_NODE_DESCRIPTION_FAILED = new TriggerDefinitionErrorType(
        101);
    public static final TriggerDefinitionErrorType OPTIONS_FAILED = new TriggerDefinitionErrorType(102);
    public static final TriggerDefinitionErrorType LISTENER_DISABLE_FAILED = new TriggerDefinitionErrorType(103);
    public static final TriggerDefinitionErrorType LISTENER_ENABLE_FAILED = new TriggerDefinitionErrorType(104);
    public static final TriggerDefinitionErrorType DYNAMIC_WEBHOOK_TRIGGER_FAILED = new TriggerDefinitionErrorType(
        105);
    public static final TriggerDefinitionErrorType POLLING_TRIGGER_FAILED = new TriggerDefinitionErrorType(106);
    public static final TriggerDefinitionErrorType DYNAMIC_WEBHOOK_DISABLE_FAILED = new TriggerDefinitionErrorType(
        107);
    public static final TriggerDefinitionErrorType DYNAMIC_WEBHOOK_ENABLE_FAILED = new TriggerDefinitionErrorType(108);
    public static final TriggerDefinitionErrorType TRIGGER_TEST_FAILED = new TriggerDefinitionErrorType(109);

    private TriggerDefinitionErrorType(int errorKey) {
        super(ActionDefinition.class, errorKey);
    }
}
