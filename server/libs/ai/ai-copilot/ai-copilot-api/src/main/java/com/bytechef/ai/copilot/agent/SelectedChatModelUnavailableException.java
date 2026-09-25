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
public class SelectedChatModelUnavailableException extends ChatModelUnavailableException {

    private final String provider;
    private final String model;
    private final long environmentId;

    public SelectedChatModelUnavailableException(String provider, String model, long environmentId) {
        super(getMessage(provider, model, environmentId));

        this.provider = provider;
        this.model = model;
        this.environmentId = environmentId;
    }

    public SelectedChatModelUnavailableException(
        String provider, String model, long environmentId, Throwable cause) {

        super(getMessage(provider, model, environmentId), cause);

        this.provider = provider;
        this.model = model;
        this.environmentId = environmentId;
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public long environmentId() {
        return environmentId;
    }

    private static String getMessage(String provider, String model, long environmentId) {
        return ("The selected model %s (%s) is not available in environment %d. Pick another model or ask an "
            + "administrator to enable it.").formatted(model, provider, environmentId);
    }
}
