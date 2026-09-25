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

package com.bytechef.platform.workflow.worker.security;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Runs task and trigger handlers of an unattended run as the owner of its job principal. A run already carrying a real
 * caller (e.g. the editor's Test button) keeps that caller.
 *
 * @author Ivica Cardic
 */
@Component
public class JobPrincipalAuthenticationRunner {

    private static final Logger log = LoggerFactory.getLogger(JobPrincipalAuthenticationRunner.class);

    private static final int MAX_WARNED_RUNS = 10_000;

    private final List<JobPrincipalAuthenticationResolver> jobPrincipalAuthenticationResolvers;
    private final Set<String> warnedRuns = Collections.newSetFromMap(
        Collections.synchronizedMap(new LinkedHashMap<>() {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_WARNED_RUNS;
            }
        }));

    public JobPrincipalAuthenticationRunner(
        List<JobPrincipalAuthenticationResolver> jobPrincipalAuthenticationResolvers) {

        List<JobPrincipalAuthenticationResolver> sortedJobPrincipalAuthenticationResolvers = new ArrayList<>(
            jobPrincipalAuthenticationResolvers);

        AnnotationAwareOrderComparator.sort(sortedJobPrincipalAuthenticationResolvers);

        this.jobPrincipalAuthenticationResolvers = List.copyOf(sortedJobPrincipalAuthenticationResolvers);
    }

    public <T> T run(
        @Nullable PlatformType type, @Nullable Long jobPrincipalId, String runDescription, Supplier<T> supplier) {

        if (!isUnattended()) {
            return supplier.get();
        }

        Authentication authentication = fetchAuthentication(type, jobPrincipalId)
            .orElseGet(() -> createSystemAuthentication(type, runDescription));

        return SecurityUtils.runAs(authentication, supplier);
    }

    private Optional<Authentication> fetchAuthentication(@Nullable PlatformType type, @Nullable Long jobPrincipalId) {
        if (type == null || jobPrincipalId == null) {
            return Optional.empty();
        }

        for (JobPrincipalAuthenticationResolver jobPrincipalAuthenticationResolver : jobPrincipalAuthenticationResolvers) {

            if (jobPrincipalAuthenticationResolver.getType() == type &&
                jobPrincipalAuthenticationResolver.isApplicable(jobPrincipalId)) {

                return jobPrincipalAuthenticationResolver.fetchAuthentication(jobPrincipalId);
            }
        }

        return Optional.empty();
    }

    private Authentication createSystemAuthentication(@Nullable PlatformType type, String runDescription) {
        if (warnedRuns.add(runDescription)) {
            log.warn(
                "No owner could be resolved for {} with principal type {}; running it as the system principal " +
                    "without admin authority",
                runDescription, type);
        }

        return UsernamePasswordAuthenticationToken.authenticated(SecurityUtils.SYSTEM_LOGIN, null, List.of());
    }

    private static boolean isUnattended() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken ||
            !SecurityUtils.isAuthenticated()) {

            return true;
        }

        return authentication.getPrincipal() instanceof String login && SecurityUtils.SYSTEM_LOGIN.equals(login);
    }
}
