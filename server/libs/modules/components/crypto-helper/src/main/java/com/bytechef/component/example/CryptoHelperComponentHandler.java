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

package com.bytechef.component.example;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.example.action.CryptoHelperGeneratePasswordAction;
import com.bytechef.component.example.action.CryptoHelperHashAction;
import com.bytechef.component.example.action.CryptoHelperHmacAction;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class CryptoHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("cryptoHelper")
        .title("Crypto Helper")
        .description("The Crypto Helper allows you to use cryptographic functions.")
        .icon("path:assets/cryptoHelper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            CryptoHelperGeneratePasswordAction.ACTION_DEFINITION,
            CryptoHelperHashAction.ACTION_DEFINITION,
            CryptoHelperHmacAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
