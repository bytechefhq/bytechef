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
 * Classifies the kind of fact stored in a {@link AiAutoMemory}. Shown in the agent's memory index and used to filter
 * entries in the management UI.
 *
 * <ul>
 * <li>{@link #USER} — user profile / preferences (role, tone, defaults).</li>
 * <li>{@link #FEEDBACK} — corrections or confirmed approaches the user asked the agent to apply in future turns.</li>
 * <li>{@link #PROJECT} — decisions, deadlines or domain constraints of the ongoing work.</li>
 * <li>{@link #REFERENCE} — pointers to external systems (dashboards, boards, external docs).</li>
 * </ul>
 *
 * <p>
 * <b>Append-only.</b> The values are persisted as INT ordinals — reordering or deleting a value would silently re-map
 * every historical memory row to the wrong type. New values MUST be appended at the end. Pinned by
 * {@code EnumOrdinalStabilityTest#testAiAutoMemoryTypeOrdinalsAreStable}.
 *
 * @author Ivica Cardic
 */
public enum AiAutoMemoryType {

    USER,
    FEEDBACK,
    PROJECT,
    REFERENCE
}
