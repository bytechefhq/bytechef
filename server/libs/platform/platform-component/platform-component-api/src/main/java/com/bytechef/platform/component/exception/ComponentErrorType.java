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

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ComponentErrorType extends AbstractErrorType {

    public static final ComponentErrorType INVALID_FILE_ENTRY = new ComponentErrorType(100);
    public static final ComponentErrorType INVALID_CONTEXT_ARGUMENT = new ComponentErrorType(101);
    public static final ComponentErrorType DATA_STORAGE_SIZE_EXCEEDED = new ComponentErrorType(102);
    public static final ComponentErrorType UNKNOWN_TRIGGER_TYPE = new ComponentErrorType(103);
    public static final ComponentErrorType COMPONENT_NOT_FOUND = new ComponentErrorType(104);
    public static final ComponentErrorType INVALID_DATE_STRING = new ComponentErrorType(105);
    public static final ComponentErrorType INVALID_ORDER_DIRECTION = new ComponentErrorType(106);
    public static final ComponentErrorType UNSUPPORTED_PROPERTY_TYPE = new ComponentErrorType(107);

    private ComponentErrorType(int errorKey) {
        super(ComponentErrorType.class, errorKey);
    }
}
