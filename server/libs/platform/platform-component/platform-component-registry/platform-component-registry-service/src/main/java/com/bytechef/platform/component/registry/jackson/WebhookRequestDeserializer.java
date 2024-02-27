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

package com.bytechef.platform.component.registry.jackson;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.registry.trigger.WebhookRequest;
import com.bytechef.platform.component.registry.trigger.WebhookRequest.WebhookBodyImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.boot.jackson.JsonComponent;

/**
 * @author Ivica Cardic
 */
@JsonComponent
public class WebhookRequestDeserializer extends JsonDeserializer<WebhookRequest> {

    private static final String BODY = "body";
    private static final String CONTENT = "content";
    private static final String MIME_TYPE = "mimeType";

    @Override
    @SuppressWarnings("unchecked")
    public WebhookRequest deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        WebhookBodyImpl webhookBody = null;

        ObjectCodec objectCodec = jp.getCodec();

        JsonNode jsonNode = objectCodec.readTree(jp);

        JsonNode bodyJsonNode = jsonNode.get(BODY);

        if (bodyJsonNode != null) {
            Object content;
            Map<String, ?> bodyMap = objectCodec.treeToValue(bodyJsonNode, Map.class);

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

            webhookBody = new WebhookBodyImpl(content, contentType, MapUtils.getString(bodyMap, MIME_TYPE));
        }

        return new WebhookRequest(
            objectCodec.treeToValue(jsonNode.get("headers"), Map.class),
            objectCodec.treeToValue(jsonNode.get("parameters"), Map.class), webhookBody,
            WebhookMethod.valueOf(getMethod(jsonNode)));
    }

    private static Map<String, Object> checkFormDataWebhookBodyContent(Map<String, ?> content) {
        return MapUtils.toMap(content, Map.Entry::getKey, WebhookRequestDeserializer::checkValue);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static Object checkFileEntry(Object item) {
        if (item instanceof Map map) {
            return new FileEntry(
                MapUtils.getRequiredString(map, "name"), MapUtils.getRequiredString(map, "extension"),
                MapUtils.getRequiredString(map, MIME_TYPE), MapUtils.getRequiredString(map, "url"));
        } else {
            return item;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object checkValue(Map.Entry<String, ?> entry) {
        if (entry.getValue() instanceof String) {
            return entry.getValue();
        } else if (entry.getValue() instanceof List<?> list) {
            return CollectionUtils.map(list, WebhookRequestDeserializer::checkFileEntry);
        } else {
            return new FileEntry((Map<String, ?>) entry.getValue());
        }
    }

    private static String getMethod(JsonNode jsonNode) {
        JsonNode fieldJsonNode = jsonNode.get("method");

        return fieldJsonNode.asText();
    }
}
