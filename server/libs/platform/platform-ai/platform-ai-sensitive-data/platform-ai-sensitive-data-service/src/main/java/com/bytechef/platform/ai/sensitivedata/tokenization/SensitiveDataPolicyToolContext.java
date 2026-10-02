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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
public final class SensitiveDataPolicyToolContext {

    public static final String KEY = "bytechef.sensitive-data-policy";

    private SensitiveDataPolicyToolContext() {
    }

    public static @Nullable SensitiveDataPolicy from(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Object value = toolContext.getContext()
            .get(KEY);

        return value instanceof SensitiveDataPolicy sensitiveDataPolicy ? sensitiveDataPolicy : null;
    }

    public static Map<String, Object> into(Map<String, Object> context, SensitiveDataPolicy policy) {
        Map<String, Object> merged = new HashMap<>(context);

        merged.put(KEY, policy);

        return merged;
    }
}
