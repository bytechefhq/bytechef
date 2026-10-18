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

package com.bytechef.ai.copilot.agent;

import com.bytechef.platform.ai.llm.NoChatProviderActivatedException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public enum CopilotRunErrorCode {

    NO_AI_PROVIDER,
    SELECTED_MODEL_UNAVAILABLE,
    MODEL_SELECTION_INCOMPLETE;

    public static @Nullable CopilotRunErrorCode of(@Nullable Throwable throwable) {
        Set<Throwable> visitedThrowables = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Throwable current = throwable; current != null && visitedThrowables.add(current); current =
            current.getCause()) {

            if (current instanceof NoAiProviderEnabledException
                || current instanceof NoChatProviderActivatedException) {
                return NO_AI_PROVIDER;
            }

            if (current instanceof SelectedChatModelUnavailableException) {
                return SELECTED_MODEL_UNAVAILABLE;
            }

            if (current instanceof IncompleteChatModelSelectionException) {
                return MODEL_SELECTION_INCOMPLETE;
            }
        }

        return null;
    }
}
