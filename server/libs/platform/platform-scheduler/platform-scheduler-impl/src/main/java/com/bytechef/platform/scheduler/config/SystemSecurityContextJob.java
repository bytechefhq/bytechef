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

package com.bytechef.platform.scheduler.config;

import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Decorates a Quartz {@link Job} so its {@code execute} runs under a system principal with admin authority. Quartz
 * worker threads carry no HTTP-derived {@link org.springframework.security.core.context.SecurityContext}, so scheduler
 * jobs invoking owner-or-admin guarded service code (e.g. an OAuth2 connection token refresh) would otherwise fail with
 * {@code AccessDeniedException}. Establishing the context centrally here keeps the individual job classes free of
 * security plumbing.
 *
 * @author Ivica Cardic
 */
record SystemSecurityContextJob(Job delegate) implements Job {

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    SystemSecurityContextJob {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            SecurityUtils.runAsSystem(() -> {
                try {
                    delegate.execute(context);
                } catch (JobExecutionException jobExecutionException) {
                    throw new JobExecutionRuntimeException(jobExecutionException);
                }

                return null;
            });
        } catch (JobExecutionRuntimeException jobExecutionRuntimeException) {
            throw jobExecutionRuntimeException.getCause();
        }
    }

    private static final class JobExecutionRuntimeException extends RuntimeException {

        private JobExecutionRuntimeException(JobExecutionException cause) {
            super(cause);
        }

        @Override
        public JobExecutionException getCause() {
            return (JobExecutionException) super.getCause();
        }
    }
}
