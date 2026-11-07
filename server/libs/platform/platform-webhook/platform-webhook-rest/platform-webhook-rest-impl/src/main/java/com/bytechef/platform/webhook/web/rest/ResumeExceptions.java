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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.platform.workflow.execution.facade.ApprovalInputException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
public final class ResumeExceptions {

    private ResumeExceptions() {
    }

    public static @Nullable ApprovalInputException findApprovalInputException(Throwable throwable) {
        return findCause(throwable, ApprovalInputException.class);
    }

    public static boolean isOptimisticLockingFailure(Throwable throwable) {
        return findCause(throwable, OptimisticLockingFailureException.class) != null;
    }

    private static <T extends Throwable> @Nullable T findCause(Throwable throwable, Class<T> causeType) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (causeType.isInstance(cause)) {
                return causeType.cast(cause);
            }
        }

        return null;
    }
}
