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

package com.bytechef.embedded.configuration.constant;

import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.platform.exception.ErrorType;

/**
 * @author Ivica Cardic
 */
public enum IntegrationErrorType implements ErrorType {

    DELETE_INTEGRATION(100), DELETE_WORKFLOW(101), UPDATE_OLD_WORKFLOW(102);

    private final int errorKey;

    IntegrationErrorType(int errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public Class<?> getErrorClass() {
        return Integration.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }
}
