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

package com.bytechef.component.object.helper.constant;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperConstants {

    public static final String ADD_KEY_VALUE_PAIRS = "addKeyValuePairs";
    public static final String ADD_VALUE_BY_KEY = "addValueByKey";
    public static final String KEY = "key";
    public static final String OBJECT_HELPER = "objectHelper";
    public static final String PARSE = "parse";
    public static final String SOURCE = "source";
    public static final String SOURCE_TYPE = "sourceType";
    public static final String STRINGIFY = "stringify";
    public static final String TYPE = "type";
    public static final List<Option<Long>> TYPE_OPTIONS = List.of(
        option("Array", 1),
        option("Boolean", 2),
        option("Date", 3),
        option("Date Time", 4),
        option("Integer", 5),
        option("Nullable", 6),
        option("Number", 7),
        option("Object", 8),
        option("String", 9),
        option("Time", 10));
    public static final String VALUE = "value";
    public static final String VALUE_TYPE = "valueType";
}
