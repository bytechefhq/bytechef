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

package com.bytechef.component.canva.action;

import static com.bytechef.component.canva.constant.CanvaConstants.ASSET_ID;
import static com.bytechef.component.canva.constant.CanvaConstants.CUSTOM;
import static com.bytechef.component.canva.constant.CanvaConstants.DESIGN_TYPE;
import static com.bytechef.component.canva.constant.CanvaConstants.HEIGHT;
import static com.bytechef.component.canva.constant.CanvaConstants.NAME;
import static com.bytechef.component.canva.constant.CanvaConstants.PRESET;
import static com.bytechef.component.canva.constant.CanvaConstants.TITLE;
import static com.bytechef.component.canva.constant.CanvaConstants.TYPE;
import static com.bytechef.component.canva.constant.CanvaConstants.TYPE_AND_ASSET;
import static com.bytechef.component.canva.constant.CanvaConstants.WIDTH;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.canva.util.CanvaUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaCreateDesignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDesign")
        .title("Create Design")
        .description("Create a Canva design.")
        .help("", "https://docs.bytechef.io/reference/components/canva_v1#create-design")
        .properties(
            string(TYPE)
                .label("Type")
                .options(
                    option("Preset", PRESET),
                    option("Custom", CUSTOM))
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name of the design type.")
                .options(
                    option("Doc", "doc"),
                    option("Email", "email"),
                    option("Presentation", "presentation"),
                    option("Whiteboard", "whiteboard"))
                .displayCondition("%s == '%s'".formatted("type", PRESET))
                .required(true),
            integer(WIDTH)
                .label("Width")
                .description("The width of the design, in pixels.")
                .minValue(40)
                .maxValue(8000)
                .displayCondition("%s == '%s'".formatted("type", CUSTOM))
                .required(true),
            integer(HEIGHT)
                .label("Height")
                .description("The height of the design, in pixels.")
                .minValue(40)
                .maxValue(8000)
                .displayCondition("%s == '%s'".formatted("type", CUSTOM))
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The name of the design.")
                .minLength(1)
                .maxLength(255)
                .required(false),
            string(ASSET_ID)
                .label("Asset Id")
                .description("The ID of an asset to insert into the created design.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("The design ID."),
                        object("owner")
                            .properties(
                                string("user_id")
                                    .description("The ID of the user."),
                                string("team_id")
                                    .description("The ID of the user's Canva Team.")),
                        array("URLs")
                            .items(
                                string("edit_url")
                                    .description("A temporary editing URL for the design."),
                                string("view_url")
                                    .description("A temporary viewing URL for the design.")),
                        dateTime("created_at")
                            .description("When the design was created in Canva."),
                        dateTime("updated_at")
                            .description("When the design was last updated in Canva."),
                        string("title")
                            .description("The design title."),
                        object("thumbnail")
                            .description("A thumbnail image representing the object.")
                            .properties(
                                integer("width")
                                    .description("The width of the thumbnail image in pixels."),
                                integer("height")
                                    .description("The height of the thumbnail image in pixels."),
                                string("url")
                                    .description("A URL for retrieving the thumbnail image.")),
                        integer("page_count")
                            .description("The total number of pages in the design."))))
        .perform(CanvaCreateDesignAction::perform)
        .processErrorResponse(CanvaUtils::processErrorResponse);

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("/designs"))
            .body(
                Body.of(
                    TYPE, TYPE_AND_ASSET,
                    DESIGN_TYPE, getDesignType(inputParameters),
                    TITLE, inputParameters.getString(TITLE),
                    ASSET_ID, inputParameters.getString(ASSET_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> getDesignType(Parameters inputParameters) {
        Map<String, Object> designType = new HashMap<>();
        String type = inputParameters.getRequiredString(TYPE);

        designType.put(TYPE, type);

        if (type.equals(PRESET)) {
            designType.put(NAME, inputParameters.getRequiredString(NAME));
        } else if (type.equals(CUSTOM)) {
            designType.put(HEIGHT, inputParameters.getRequiredInteger(HEIGHT));
            designType.put(WIDTH, inputParameters.getRequiredInteger(WIDTH));
        }

        return designType;
    }
}
