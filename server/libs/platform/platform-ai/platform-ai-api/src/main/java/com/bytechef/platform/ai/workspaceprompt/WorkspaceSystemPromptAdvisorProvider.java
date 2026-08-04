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

package com.bytechef.platform.ai.workspaceprompt;

import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * CE-side SPI seam so a component running on the classpath without any EE module (e.g. the canvas AI Agent component)
 * can obtain a workspace-bound system-prompt advisor without depending on the EE
 * {@code com.bytechef.ee.platform.ai.workspaceprompt} module directly — the same idiom as
 * {@code AiGuardrailsAdvisorProvider}. When no EE implementation is registered, or the resolved workspace has no
 * prompt, callers simply skip attaching an advisor.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceSystemPromptAdvisorProvider {

    /**
     * Returns an {@link Advisor} bound to the workspace resolved for {@code jobPrincipalId}, or empty when no workspace
     * system prompt applies (no EE implementation, non-AUTOMATION run, unresolvable workspace, or no prompt set).
     *
     * @param platformType   the platform the run belongs to, or {@code null} when the calling context carries none
     * @param jobPrincipalId the run's job principal id (a project deployment id), or {@code null} when unknown
     * @param surface        identifies the calling surface (e.g. {@code "ai_agent"}); reserved for telemetry
     * @return the workspace system prompt advisor, or empty when none applies
     */
    Optional<Advisor> getAdvisor(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface);
}
