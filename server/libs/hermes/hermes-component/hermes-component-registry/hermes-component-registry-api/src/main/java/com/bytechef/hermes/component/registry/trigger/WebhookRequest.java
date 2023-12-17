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
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WebhookRequest(
    Map<String, String[]> headers, Map<String, String[]> parameters, WebhookBodyImpl body, WebhookMethod method) {

    public static final String WEBHOOK_REQUEST = "webhookRequest";

    @Override
    public String toString() {
        return "WebhookRequest{" +
            "headers=" + MapUtils.toString(headers) +
            ", parameters=" + MapUtils.toString(parameters) +
            ", body=" + body +
            ", method=" + method +
            '}';
    }

    public static class WebhookBodyImpl implements WebhookBody {

        private Object content;
        private ContentType contentType;
        private String mimeType;

        private WebhookBodyImpl() {
        }

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
    }
}
