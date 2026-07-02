/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.dto;

import com.bytechef.platform.ai.llm.Provider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiDefaultModelWithApiKeyDTO(Provider provider, String model, String apiKey) {
}
