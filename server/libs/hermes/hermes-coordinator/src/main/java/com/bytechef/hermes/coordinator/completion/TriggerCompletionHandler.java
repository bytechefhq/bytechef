
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.coordinator.completion;

import com.bytechef.commons.util.ExceptionUtils;
import com.bytechef.error.ErrorHandler;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.trigger.TriggerExecution;

import java.util.Arrays;

/**
 * @author Ivica Cardic
 */
public class TriggerCompletionHandler {

    private final ErrorHandler<? super Errorable> errorHandler;

    public TriggerCompletionHandler(ErrorHandler<? super Errorable> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void handle(TriggerExecution triggerExecution) {
        try {
            // TODO
            System.out.println(triggerExecution);
        } catch (Exception e) {
            triggerExecution.setError(
                new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            errorHandler.handle(triggerExecution);
        }
    }
}
