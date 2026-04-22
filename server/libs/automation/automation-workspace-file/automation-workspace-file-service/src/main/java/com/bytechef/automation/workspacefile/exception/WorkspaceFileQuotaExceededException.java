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

package com.bytechef.automation.workspacefile.exception;

/**
 * @author Ivica Cardic
 */
public class WorkspaceFileQuotaExceededException extends RuntimeException {

    private final long attempted;
    private final long limit;

    public WorkspaceFileQuotaExceededException(String message, long attempted, long limit) {
        super(message);

        this.attempted = attempted;
        this.limit = limit;
    }

    public long getAttempted() {
        return attempted;
    }

    public long getLimit() {
        return limit;
    }
}
