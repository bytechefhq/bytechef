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

package com.bytechef.component.productboard.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.productboard.util.ProductboardUtils.getAllNotes;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.productboard.property.ProductboardExpandedNoteProperties;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ProductboardListNotesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listNotes")
        .title("List All Notes")
        .description("Returns detail of all notes order by created_at desc")
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(ProductboardExpandedNoteProperties.PROPERTIES))))
        .perform(ProductboardListNotesAction::perform);

    private ProductboardListNotesAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return getAllNotes(context);
    }
}
