/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.example.customcomponent;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;

/**
 * @version ee
 */
public class LazyHelperComponentHandler implements ComponentHandler {

    @Override
    public ComponentDefinition getDefinition() {
        return ComponentDsl.component("lazyHelper")
            .actions(
                ComponentDsl.action("greet")
                    .perform(LazyHelperComponentHandler::perform));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return LazyHelper.greet("ByteChef");
    }
}
