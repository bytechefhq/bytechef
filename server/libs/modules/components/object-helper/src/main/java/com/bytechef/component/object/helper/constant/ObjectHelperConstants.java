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

package com.bytechef.component.object.helper.constant;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperConstants {

    private ObjectHelperConstants() {
    }

    public static final String KEY = "key";
    public static final String LIST = "list";
    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String TYPE = "type";
    public static final String VALUE = "value";

    public static final List<Option<String>> TYPE_OPTIONS = List.of(
        option("Array", ValueType.ARRAY.name()),
        option("Boolean", ValueType.BOOLEAN.name()),
        option("Date", ValueType.DATE.name()),
        option("Date Time", ValueType.DATE_TIME.name()),
        option("Integer", ValueType.INTEGER.name()),
        option("Nullable", ValueType.NULL.name()),
        option("Number", ValueType.NUMBER.name()),
        option("Object", ValueType.OBJECT.name()),
        option("String", ValueType.STRING.name()),
        option("Time", ValueType.TIME.name()));
}
