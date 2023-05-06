
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
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookParameters;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WebhookParametersImpl implements WebhookParameters {

    private final Map<String, String[]> parameters;

    public WebhookParametersImpl(Map<String, String[]> parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    @Override
    public String getValue(String name) {
        String[] values = parameters.get(name);

        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public String[] getValues(String name) {
        return parameters.get(name);
    }

    @Override
    public String toString() {
        return "WebhookParametersImpl{" +
            "parameters=" + parameters +
            '}';
    }

    public static class WebhookParametersConverter implements Converter<Map<String, Object>, WebhookParameters> {

        @Override
        public WebhookParameters convert(Map<String, Object> source) {
            return new WebhookParametersImpl(MapValueUtils.getMap(source, "parameters"));
        }
    }
}
