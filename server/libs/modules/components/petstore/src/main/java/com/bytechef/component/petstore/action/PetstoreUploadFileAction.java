
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.petstore.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseType;

import com.bytechef.component.petstore.property.PetstoreApiResponseProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreUploadFileAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("uploads an image")
        .description("")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/pet/{petId}/uploadImage", "bodyContentType", BodyContentType.BINARY, "mimeType",
                "application/octet-stream"

            ))
        .properties(integer("petId").label("Pet Id")
            .description("ID of pet to update")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("additionalMetadata").label("Additional Metadata")
                .description("Additional Metadata")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            fileEntry("fileEntry").metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(PetstoreApiResponseProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
