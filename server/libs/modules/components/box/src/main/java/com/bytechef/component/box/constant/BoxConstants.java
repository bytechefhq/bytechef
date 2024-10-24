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

package com.bytechef.component.box.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Domiter
 */
public class BoxConstants {

    private BoxConstants() {
    }

    public static final String FILE = "file";
    public static final String FILE_ID = "fileId";
    public static final String FOLDER = "folder";
    public static final String FOLDER_ID = "folderId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String PARENT = "parent";
    public static final String TYPE = "type";

    public static final ModifiableObjectProperty FILE_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE),
            string(ID),
            string(NAME),
            object(PARENT)
                .properties(
                    string(TYPE),
                    string(ID),
                    string(NAME)));
}
