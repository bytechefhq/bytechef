/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlas.json.converter;

import com.atlas.json.JSONObjectUtil;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class JSONObjectConverter implements Converter<Map<?, ?>, JSONObject> {

    @Override
    public JSONObject convert(Map<?, ?> source) {
        return JSONObjectUtil.of(source);
    }
}
