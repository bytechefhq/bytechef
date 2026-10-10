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

/**
 * @author Ivica Cardic
 */
public class ChatModelUnavailableException extends RuntimeException {

    public ChatModelUnavailableException(String message) {
        super(message);
    }

    public ChatModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ChatModelUnavailableException noProviderEnabled(long environmentId) {
        return new ChatModelUnavailableException(getNoProviderEnabledMessage(environmentId));
    }

    public static ChatModelUnavailableException noProviderEnabled(long environmentId, Throwable cause) {
        return new ChatModelUnavailableException(getNoProviderEnabledMessage(environmentId), cause);
    }

    private static String getNoProviderEnabledMessage(long environmentId) {
        return ("No AI provider is enabled in environment %d. Ask an administrator to enable an AI provider with a "
            + "chat model.").formatted(environmentId);
    }
}
