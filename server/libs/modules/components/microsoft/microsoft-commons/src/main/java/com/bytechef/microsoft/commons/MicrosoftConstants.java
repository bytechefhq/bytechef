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

package com.bytechef.microsoft.commons;

import static com.bytechef.component.definition.ComponentDsl.bool;

import com.bytechef.component.definition.Property.BooleanProperty;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftConstants {

    public static final String FILE = "file";
    public static final String ID = "id";
    public static final String LAST_TIME_CHECKED = "lastTimeChecked";
    public static final String NAME = "name";
    public static final String ODATA_NEXT_LINK = "@odata.nextLink";
    public static final String RECURSIVE = "recursive";
    public static final String VALUE = "value";

    public static final BooleanProperty RECURSIVE_PROPERTY = bool(RECURSIVE)
        .label("Recursive")
        .description(
            "Whether to watch subfolders recursively. If false, only the specified folder will be watched. " +
                "May return many results.")
        .defaultValue(false)
        .required(false);
}
