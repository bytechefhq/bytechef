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

package com.bytechef.automation.configuration.exception;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.platform.exception.ErrorType;

/**
 * @author Ivica Cardic
 */
public enum ProjectInstanceErrorType implements ErrorType {

    CREATE_PROJECT_INSTANCE(100), DELETE_PROJECT_INSTANCE(101);

    private final int errorKey;

    ProjectInstanceErrorType(int errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public Class<?> getErrorClass() {
        return ProjectInstance.class;
    }

    @Override
    public int getErrorKey() {
        return errorKey;
    }
}
