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

import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * CE-side SPI seam so a component running on the classpath without any EE module (e.g. the canvas AI Agent component)
 * can obtain a workspace-bound content-guardrails advisor without depending on the EE
 * {@code com.bytechef.ee.platform.ai.guardrails} module directly. Consumers pull an implementation through an optional
 * Spring bean (see {@code ToolExecutionRecorder} for the same CE-SPI/EE-impl idiom) — when no EE guardrails
 * implementation is registered, or when guardrails are not applicable, callers simply skip attaching an advisor.
 *
 * @author Ivica Cardic
 */
public interface AiGuardrailsAdvisorProvider {

    /**
     * Returns an {@link Advisor} bound to the workspace resolved for {@code jobPrincipalId}, or empty when guardrails
     * are not applicable for this call (no EE implementation, or every guardrail category is disabled for the resolved
     * workspace).
     *
     * @param platformType   the platform the run belongs to (e.g. {@code AUTOMATION} or {@code EMBEDDED}), or
     *                       {@code null} when the calling context carries none (treated as not-AUTOMATION, so the
     *                       tenant-default workspace applies)
     * @param jobPrincipalId the run's job principal id (e.g. a project deployment id), or {@code null} when unknown
     * @param surface        identifies the calling surface for metrics/telemetry (e.g. {@code "ai_agent"})
     * @return the guardrails advisor, or empty when none applies
     */
    Optional<Advisor> getAdvisor(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface);
}
