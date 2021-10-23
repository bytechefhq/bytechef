/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.workflow.core.context;

import com.integri.atlas.workflow.core.MapObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapContext extends MapObject implements Context {

    public MapContext() {
        super(new HashMap<>());
    }

    public MapContext(String aKey, Object aValue) {
        this(Collections.singletonMap(aKey, aValue));
    }

    public MapContext(Map<String, Object> aSource) {
        super(aSource);
    }

    public MapContext(Context aSource) {
        super(aSource.asMap());
    }
}
