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

package com.bytechef.component.google.workspace.admin.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.LICENSE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.PRODUCT_ID;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.SKU_ID;
import static com.bytechef.component.google.workspace.admin.constant.GoogleWorkspaceAdminConstants.USER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleWorkspaceAdminAssignLicenseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("assignLicense")
        .title("Assign License")
        .description("Assigns a product license to a user.")
        .properties(
            string(USER_ID)
                .label("User email")
                .description("The user's current primary email address.")
                .required(true),
            string(PRODUCT_ID)
                .label("Product ID")
                .description(
                    "A product's unique identifier. Use this documentation to find product ID: https://developers.google.com/workspace/admin/licensing/v1/how-tos/products.")
                .required(true),
            string(SKU_ID)
                .label("SKU ID")
                .description(
                    "A SKU's unique identifier. Use this documentation to find SKU ID: https://developers.google.com/workspace/admin/licensing/v1/how-tos/products.")
                .required(true))
        .output(outputSchema(LICENSE_OUTPUT_PROPERTY))
        .perform(GoogleWorkspaceAdminAssignLicenseAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleWorkspaceAdminAssignLicenseAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("https://licensing.googleapis.com/apps/licensing/v1/product/" +
                inputParameters.getRequiredString(PRODUCT_ID) + "/sku/" +
                inputParameters.getRequiredString(SKU_ID) + "/user"))
            .configuration(responseType(ResponseType.JSON))
            .body(
                Body.of(Map.of(USER_ID, inputParameters.getRequiredString(USER_ID))))
            .execute()
            .getBody();
    }
}
