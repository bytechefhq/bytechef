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

package com.bytechef.platform.component.jackson;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * @author Ivica Cardic
 */
@JacksonComponent
public class WebhookRequestDeserializer extends ValueDeserializer<WebhookRequest> {

    private static final String BODY = "body";
    private static final String CONTENT = "content";
    private static final String MIME_TYPE = "mimeType";
    private static final String RAW_CONTENT = "rawContent";

    @Override
    public WebhookRequest deserialize(JsonParser jp, DeserializationContext ctxt) {
        WebhookBodyImpl webhookBody = null;

        JsonNode jsonNode = ctxt.readTree(jp);

        JsonNode bodyJsonNode = jsonNode.get(BODY);

        if (bodyJsonNode != null && !bodyJsonNode.isNull()) {
            Object content;
            @SuppressWarnings("unchecked")
            Map<String, ?> bodyMap = ctxt.readTreeAsValue(bodyJsonNode, Map.class);

            ContentType contentType = ContentType.valueOf(MapUtils.getString(bodyMap, "contentType"));

            if (contentType == ContentType.BINARY) {
                content = MapUtils.get(bodyMap, CONTENT, FileEntry.class);
            } else if (contentType == ContentType.FORM_DATA) {
                content = checkFormDataWebhookBodyContent(MapUtils.getRequiredMap(bodyMap, CONTENT));
            } else if (contentType == ContentType.FORM_URL_ENCODED || contentType == ContentType.JSON ||
                contentType == ContentType.XML) {

                if (bodyMap.get(CONTENT) instanceof List<?> list) {
                    content = list;
                } else {
                    content = MapUtils.getRequiredMap(bodyMap, CONTENT);
                }

            } else {
                content = MapUtils.getRequiredString(bodyMap, CONTENT);
            }

            webhookBody = new WebhookBodyImpl(
                content, contentType, MapUtils.getString(bodyMap, MIME_TYPE), MapUtils.getString(bodyMap, RAW_CONTENT));
        }

        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = ctxt.readTreeAsValue(jsonNode.get("headers"), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> parameters = ctxt.readTreeAsValue(jsonNode.get("parameters"), Map.class);

        return new WebhookRequest(headers, parameters, webhookBody, WebhookMethod.valueOf(getMethod(jsonNode)));
    }

    private static Map<String, Object> checkFormDataWebhookBodyContent(Map<String, ?> content) {
        return MapUtils.toMap(content, Map.Entry::getKey, WebhookRequestDeserializer::checkValue);
    }

    private static Object checkListItem(Object item) {
        if (item instanceof Map<?, ?> map) {
            return checkValue(map);
        } else if (item instanceof List<?> list) {
            return CollectionUtils.map(list, WebhookRequestDeserializer::checkListItem);
        } else {
            return item;
        }
    }

    private static Object checkValue(Map.Entry<?, ?> entry) {
        if (entry.getValue() instanceof Map<?, ?> map) {
            return checkValue(map);
        } else if (entry.getValue() instanceof List<?> list) {
            return CollectionUtils.map(list, WebhookRequestDeserializer::checkListItem);
        } else {
            return entry.getValue();
        }
    }

    @NonNull
    private static Object checkValue(Map<?, ?> map) {
        if (FileEntry.isFileEntryMap(map)) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> objectMap = (Map<Object, Object>) map;

            return new FileEntryImpl(
                MapUtils.getRequiredString(objectMap, "name"), MapUtils.getString(objectMap, "extension"),
                MapUtils.getString(objectMap, MIME_TYPE), MapUtils.getRequiredString(objectMap, "url"));
        } else {
            return MapUtils.toMap(map, Map.Entry::getKey, WebhookRequestDeserializer::checkValue);
        }
    }

    private static String getMethod(JsonNode jsonNode) {
        JsonNode fieldJsonNode = jsonNode.get("method");

        return fieldJsonNode.asString();
    }

    private static class FileEntryImpl implements com.bytechef.component.definition.FileEntry {

        private String extension;
        private String mimeType;
        private String name;
        private String url;

        private FileEntryImpl() {
        }

        public FileEntryImpl(String name, String extension, String mimeType, String url) {
            this.extension = Objects.requireNonNull(extension);
            this.mimeType = Objects.requireNonNull(mimeType);
            this.name = Objects.requireNonNull(name);
            this.url = Objects.requireNonNull(url);
        }

        @Override

        public String getExtension() {
            return extension;
        }

        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "FileEntryImpl{" +
                "name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", url='" + url + '\'' +
                '}';
        }
    }
}
