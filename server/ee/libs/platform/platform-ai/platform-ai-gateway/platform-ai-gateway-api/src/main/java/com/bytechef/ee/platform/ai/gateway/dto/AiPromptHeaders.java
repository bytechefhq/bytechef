/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiPromptHeaders(@Nullable String promptName, @Nullable String environment) {

    public static final String HEADER_PROMPT_NAME = "X-ByteChef-Prompt-Name";
    public static final String HEADER_PROMPT_ENVIRONMENT = "X-ByteChef-Prompt-Environment";

    public static final String DEFAULT_ENVIRONMENT = "production";

    public boolean hasPromptEnabled() {
        return promptName != null;
    }

    public String resolvedEnvironment() {
        return environment != null ? environment : DEFAULT_ENVIRONMENT;
    }
}
