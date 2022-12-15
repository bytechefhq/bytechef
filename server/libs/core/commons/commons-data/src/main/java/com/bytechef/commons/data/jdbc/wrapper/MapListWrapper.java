
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

package com.bytechef.commons.data.jdbc.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * See https://github.com/spring-projects/spring-data-relational/issues/867
 *
 * @author Ivica Cardic
 */
public class MapListWrapper {

    private List<Map<String, Object>> list = Collections.emptyList();

    public MapListWrapper() {
    }

    public MapListWrapper(List<Map<String, Object>> list) {
        this.list = new ArrayList<>(list.stream()
            .map(HashMap::new)
            .toList());
    }

    public List<Map<String, Object>> getList() {
        return new ArrayList<>(list);
    }

    @Override
    public String toString() {
        return "ListMapWrapper{" + "list=" + list + '}';
    }
}
