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

package com.bytechef.platform.ai.auto.memory;

/**
 * Discriminates the owner kind of an {@link AiAutoMemory} row so user-owned (AI Hub) and deployment-owned (workflow
 * agent) memory can share the {@code ai_auto_memory} table without colliding.
 *
 * <p>
 * <b>Append-only.</b> Persisted as an INT ordinal in {@code ai_auto_memory.principal_type}; reordering or removing a
 * value would silently re-map historical rows. New values MUST be appended at the end. Pinned by
 * {@code EnumOrdinalStabilityTest#testAiAutoMemoryPrincipalTypeOrdinalsAreStable}.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum AiAutoMemoryPrincipalType {

    // append-only
    USER,
    DEPLOYMENT,
    INTEGRATION_INSTANCE
}
