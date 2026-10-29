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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.execution.domain.Job;

/**
 * Decides, synchronously at the transition to {@link Job.Status#STARTED}, whether a job may run. Called on every start
 * and on every restart of a {@link Job.Status#FAILED} or {@link Job.Status#PAYMENT_REQUIRED} job; a job that is refused
 * ends in {@link Job.Status#PAYMENT_REQUIRED}.
 *
 * @author Matija Petanjek
 */
public interface JobStartGuard {

    /**
     * @return {@code false} when the job must not run because payment is required
     */
    boolean tryStart(Job job);
}
