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

package com.bytechef.component.microsoft.share.point.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointConstants {

    public static final String COLUMNS = "columns";
    public static final String DESCRIPTION = "description";
    public static final String DISPLAY_NAME = "displayName";
    public static final String FIELDS = "fields";
    public static final String FILE = "file";
    public static final String FOLDER = "folder";
    public static final String ID = "id";
    public static final String LIST_ID = "listId";
    public static final String NAME = "name";
    public static final String PARENT_FOLDER = "parentFolder";
    public static final String READ_ONLY = "readOnly";
    public static final String REQUIRED = "required";
    public static final String SITE_ID = "siteId";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty SITE_ID_PROPERTY = string(SITE_ID)
        .label("Site ID")
        .options((OptionsFunction<String>) MicrosoftSharePointUtils::getSiteOptions)
        .required(true);

    private MicrosoftSharePointConstants() {
    }
}
