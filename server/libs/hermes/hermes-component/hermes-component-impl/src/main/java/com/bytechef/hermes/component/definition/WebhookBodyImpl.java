
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WebhookBodyImpl implements WebhookBody {

    private final Object content;
    private final ContentType contentType;
    private final String mimeType;

    public WebhookBodyImpl(Object content, ContentType contentType, String mimeType) {
        this.content = content;
        this.contentType = contentType;
        this.mimeType = mimeType;
    }

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

    @Override
    public String toString() {
        return "WebhookBodyImpl{" +
            "content=" + content +
            ", contentType=" + contentType +
            ", mimeType='" + mimeType + '\'' +
            '}';
    }

    public static class WebhookBodyConverter implements Converter<Map<String, Object>, WebhookBody> {

        @Override
        public WebhookBody convert(Map<String, Object> source) {
            return new WebhookBodyImpl(
                MapValueUtils.get(source, "content"), MapValueUtils.get(source, "content", ContentType.class),
                MapValueUtils.getString(source, "contentType"));
        }
    }
}
