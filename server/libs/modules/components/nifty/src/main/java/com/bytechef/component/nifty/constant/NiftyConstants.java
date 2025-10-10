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

package com.bytechef.component.nifty.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.nifty.util.NiftyUtils;

/**
 * @author Luka Ljubić
 */
public class NiftyConstants {

    public static final String APP_ID = "app_id";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String PROJECT = "project";

    public static final ModifiableStringProperty PROJECT_PROPERTY = string(PROJECT)
        .label("Project ID")
        .description("ID of the project within which the task will be created.")
        .options((OptionsFunction<String>) NiftyUtils::getProjectIdOptions)
        .required(true);

    private NiftyConstants() {
    }
}
