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

package com.bytechef.component.supabase;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.supabase.action.SupabaseUploadFileAction;
import com.bytechef.component.supabase.connection.SupabaseConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class SupabaseComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("supabase")
        .title("Supabase")
        .description(
            "Supabase is an open source Firebase alternative. Start your project with a Postgres database, " +
                "Authentication, instant APIs, Edge Functions, Realtime subscriptions, Storage, and Vector embeddings.")
        .icon("path:assets/supabase.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(SupabaseConnection.CONNECTION_DEFINITION)
        .actions(SupabaseUploadFileAction.ACTION_DEFINITION)
        .clusterElements(tool(SupabaseUploadFileAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
