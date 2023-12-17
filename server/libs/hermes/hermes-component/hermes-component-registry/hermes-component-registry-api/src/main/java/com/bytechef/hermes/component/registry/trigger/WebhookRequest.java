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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
        private static final String MIME_TYPE = "mimeType";

        @Override
        public WebhookRequest convert(Map<String, Object> source) {
            WebhookRequest.WebhookBodyImpl webhookBody = null;

            if (MapUtils.containsKey(source, BODY)) {
                Object content;
                Map<String, ?> bodyMap = MapUtils.getRequiredMap(source, BODY);

                ContentType contentType = ContentType.valueOf(MapUtils.getString(bodyMap, "contentType"));

                if (contentType == ContentType.BINARY) {
                    content = MapUtils.get(bodyMap, CONTENT, FileEntry.class);
                } else if (contentType == ContentType.FORM_DATA) {
                    content = checkFormDataWebhookBodyContent(MapUtils.getRequiredMap(bodyMap, CONTENT));
                } else if (contentType == ContentType.FORM_URL_ENCODED || contentType == ContentType.JSON ||
                    contentType == ContentType.XML) {

                    content = MapUtils.getRequiredMap(bodyMap, CONTENT);
                } else {
                    content = MapUtils.getRequiredString(bodyMap, CONTENT);
                }

                webhookBody =
                    new WebhookRequest.WebhookBodyImpl(content, contentType, MapUtils.getString(bodyMap, MIME_TYPE));
            }

            return new WebhookRequest(
                MapUtils.getMap(source, "headers", new ParameterizedTypeReference<>() {}, Map.of()),
                MapUtils.getMap(source, "parameters", new ParameterizedTypeReference<>() {}, Map.of()),
                webhookBody, WebhookMethod.valueOf(MapUtils.getString(source, "method")));
        }

        private static Map<String, Object> checkFormDataWebhookBodyContent(Map<String, ?> content) {
            return MapUtils.toMap(
                content,
                Map.Entry::getKey,
                entry -> {
                    if (entry.getValue() instanceof String) {
                        return entry.getValue();
                    } else if (entry.getValue() instanceof List<?> list) {
                        return CollectionUtils.map(
                            list,
                            item -> {
                                if (item instanceof Map<?, ?> map) {
                                    return new FileEntry(
                                        (String) map.get("name"), (String) map.get("extension"),
                                        (String) map.get(MIME_TYPE), (String) map.get("url"));
                                } else {
                                    return item;
                                }
                            });
                    } else {
                        return MapUtils.getRequiredMap(content, entry.getKey(), FileEntry.class);
                    }
                });
        }
    }

    @Override
    public String toString() {
        return "WebhookRequest{" +
            "headers=" + MapUtils.toString(headers) +
            ", parameters=" + MapUtils.toString(parameters) +
            ", body=" + body +
            ", method=" + method +
            '}';
    }

    public record WebhookBodyImpl(
        Object content, ContentType contentType, String mimeType) implements WebhookBody {

        @Override
        public Object getContent() {
            return content;
        }

        @Override
        public ContentType getContentType() {
            return contentType;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }
    }
}
