/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.component.registry.trigger;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WebhookRequest(
    Map<String, String[]> headers, Map<String, String[]> parameters, WebhookBodyImpl body, WebhookMethod method) {

    public static final String WEBHOOK_REQUEST = "webhookRequest";

    public static class WebhookRequestConverter implements Converter<Map<String, Object>, WebhookRequest> {

        private static final String BODY = "body";
        private static final String CONTENT = "content";

        @Override
        public WebhookRequest convert(Map<String, Object> source) {
            WebhookRequest.WebhookBodyImpl webhookBody = null;

            if (MapUtils.containsKey(source, BODY)) {
                Map<String, ?> bodyMap = MapUtils.getRequiredMap(source, BODY);

                Object content;
                ContentType contentType = ContentType.valueOf(MapUtils.getString(bodyMap, "contentType"));

                if (contentType == ContentType.BINARY) {
                    content = MapUtils.get(bodyMap, CONTENT, FileEntry.class);
                } else if (contentType == ContentType.FORM_DATA) {
                    content = checkContent(bodyMap);
                } else if (contentType == ContentType.FORM_URL_ENCODED || contentType == ContentType.JSON ||
                    contentType == ContentType.XML) {

                    content = MapUtils.getRequiredMap(bodyMap, CONTENT);
                } else {
                    content = MapUtils.getRequiredString(bodyMap, CONTENT);
                }

                webhookBody =
                    new WebhookRequest.WebhookBodyImpl(content, contentType, MapUtils.getString(bodyMap, "mimeType"));
            }

            return new WebhookRequest(
                MapUtils.getMap(source, "headers", new ParameterizedTypeReference<>() {}, Map.of()),
                MapUtils.getMap(source, "parameters", new ParameterizedTypeReference<>() {}, Map.of()),
                webhookBody, WebhookMethod.valueOf(MapUtils.getString(source, "method")));
        }

        private static Map<String, Object> checkContent(Map<String, ?> bodyMap) {
            Map<String, ?> content = MapUtils.getRequiredMap(bodyMap, CONTENT);

            return MapUtils.toMap(
                content,
                Map.Entry::getKey,
                entry -> {
                    if (entry.getValue() instanceof String) {
                        return MapUtils.getRequiredString(bodyMap, entry.getKey());
                    } else {
                        return MapUtils.getRequiredMap(bodyMap, entry.getKey(), FileEntry.class);
                    }
                });
        }

    }

    public record WebhookBodyImpl(
        Object content, ContentType contentType, String mimeType) implements WebhookBody {
    }
}
