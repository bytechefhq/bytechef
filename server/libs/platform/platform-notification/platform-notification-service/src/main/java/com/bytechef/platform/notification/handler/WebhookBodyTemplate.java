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

package com.bytechef.platform.notification.handler;

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the admin-defined webhook body template. Each {@code {{path}}} placeholder is replaced by the value at the
 * dotted path of the delivery summary, escaped as the content of a JSON string. The literal path {@code summary}
 * inserts the whole summary as raw JSON.
 *
 * @author Ivica Cardic
 */
public final class WebhookBodyTemplate {

    private static final int MAX_TEMPLATE_LENGTH = 10000;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");
    private static final String SUMMARY_PATH = "summary";

    private WebhookBodyTemplate() {
    }

    public static String render(String template, Map<String, Object> summary) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        return matcher.replaceAll(matchResult -> Matcher.quoteReplacement(resolve(matchResult.group(1), summary)));
    }

    public static void validate(String template) {
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            throw new IllegalArgumentException(
                "Webhook body template must have at most %d characters".formatted(MAX_TEMPLATE_LENGTH));
        }

        String rendered = render(template, NotificationHandlerContext.sample()
            .toMap());

        try {
            JsonUtils.read(rendered);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("Webhook body template must render to valid JSON");
        }
    }

    private static String resolve(String path, Map<String, Object> summary) {
        if (SUMMARY_PATH.equals(path)) {
            return JsonUtils.write(summary);
        }

        Object value = summary;

        for (String segment : path.split("\\.", -1)) {
            if (!(value instanceof Map<?, ?> map)) {
                return "";
            }

            value = map.get(segment);
        }

        if (value == null) {
            return "";
        }

        String json = JsonUtils.write(value instanceof Map<?, ?> || value instanceof Iterable<?>
            ? JsonUtils.write(value) : String.valueOf(value));

        return json.substring(1, json.length() - 1);
    }
}
