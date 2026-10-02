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

import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public final class PublishedInputSpans {

    public static final String CONTEXT_KEY = "bytechef.guardrails.publishedInputSpans";

    private PublishedInputSpans() {
    }

    public static List<SensitiveSpan> from(Map<String, ?> context) {
        Object value = context.get(CONTEXT_KEY);

        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<SensitiveSpan> spans = new ArrayList<>(list.size());

        for (Object element : list) {
            if (!(element instanceof SensitiveSpan span)) {
                return List.of();
            }

            spans.add(span);
        }

        return List.copyOf(spans);
    }
}
