/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.action;

import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformResponseToApiRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("responseToAPIRequest")
        .title("Response to API Request")
        .description("Converts the response to API request.")
        .properties()
        .output()
        .perform(ApiPlatformResponseToApiRequestAction::perform);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return null;
    }
}
