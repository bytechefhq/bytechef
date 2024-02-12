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

package com.bytechef.component.data.stream.action;

import static com.bytechef.component.definition.ComponentDSL.action;

import com.bytechef.component.definition.ComponentDSL;

/**
 * @author Ivica Cardic
 */
public class DataStreamSyncAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("sync")
        .title("Sync Data Stream")
        .description("Sync large volume of data between source and destination applications.");
}
