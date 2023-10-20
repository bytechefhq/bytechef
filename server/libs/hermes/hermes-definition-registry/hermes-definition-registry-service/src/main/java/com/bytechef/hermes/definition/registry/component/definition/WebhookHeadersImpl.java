
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

package com.bytechef.hermes.definition.registry.component.definition;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookHeaders;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WebhookHeadersImpl implements WebhookHeaders {

    private final Map<String, String[]> headers;

    public WebhookHeadersImpl(Map<String, String[]> headers) {
        this.headers = new HashMap<>(headers);
    }

    @Override
    public String getValue(String name) {
        String[] values = headers.get(name);

        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public String[] getValues(String name) {
        return headers.get(name);
    }

    @Override
    public String toString() {
        return "WebhookHeadersImpl{" +
            "headers=" + headers +
            '}';
    }

    public static class WebhookHeadersConverter implements Converter<Map<String, Object>, WebhookHeaders> {

        @Override
        public WebhookHeaders convert(Map<String, Object> source) {
            return new WebhookHeadersImpl(MapValueUtils.getMap(source, "headers"));
        }
    }
}
