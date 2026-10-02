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

package com.bytechef.platform.ai.guardrails;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * @author Ivica Cardic
 */
public interface AiGuardrailsAdvisorProvider {

    default Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        return getAdvisor(platformType, jobPrincipalId, surface, RestorationDestination.CONVERSATION);
    }

    Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface,
        RestorationDestination destination);

    @Nullable
    SensitiveDataMetrics getMetrics(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface);

    default Optional<Advisor> getAdvisorForWorkspace(@Nullable Long workspaceId, String surface) {
        return getAdvisorForWorkspace(workspaceId, surface, RestorationDestination.CONVERSATION);
    }

    Optional<Advisor> getAdvisorForWorkspace(
        @Nullable Long workspaceId, String surface, RestorationDestination destination);

    @Nullable
    SensitiveDataMetrics getMetricsForWorkspace(@Nullable Long workspaceId, String surface);
}
