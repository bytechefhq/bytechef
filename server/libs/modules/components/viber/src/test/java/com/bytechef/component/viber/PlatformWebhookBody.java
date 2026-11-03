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

package com.bytechef.component.viber;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Builds a JSON {@link WebhookBody} the way the platform's webhook request parser does, honouring the trigger
 * definition's raw body flag.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record PlatformWebhookBody(Object content, String rawContent) implements WebhookBody {

    public static PlatformWebhookBody of(String rawContent, TriggerDefinition triggerDefinition) {
        Object content = triggerDefinition.getWebhookRawBody() ? rawContent : JsonUtils.read(rawContent);

        return new PlatformWebhookBody(content, rawContent);
    }

    @Override
    public Object getContent() {
        return content;
    }

    @Override
    public <T> T getContent(Class<T> valueType) {
        return ConvertUtils.convertValue(content, valueType);
    }

    @Override
    public <T> T getContent(TypeReference<T> valueTypeRef) {
        return ConvertUtils.convertValue(content, valueTypeRef.getType());
    }

    @Override
    public ContentType getContentType() {
        return ContentType.JSON;
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String getRawContent() {
        return rawContent;
    }
}
