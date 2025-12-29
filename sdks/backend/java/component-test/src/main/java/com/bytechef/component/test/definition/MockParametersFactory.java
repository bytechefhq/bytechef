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

package com.bytechef.component.test.definition;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Igor Beslic
 */
public class MockParametersFactory {

    static {
        MapUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    public static Parameters create(Map<String, Object> map) {
        try {
            return new MockParametersImpl(map);
        } catch (Exception exception) {
            throw new RuntimeException("Unable to instantiate Parameters", exception);
        }
    }
}
