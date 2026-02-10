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

package com.bytechef.component.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.helper.action.CryptoHelperHashAction;
import com.bytechef.component.helper.action.CryptoHelperHmacAction;
import com.bytechef.component.helper.action.CryptoHelperPgpDecryptAction;
import com.bytechef.component.helper.action.CryptoHelperPgpEncryptAction;
import com.bytechef.component.helper.action.CryptoHelperRsaDecryptAction;
import com.bytechef.component.helper.action.CryptoHelperRsaEncryptAction;
import com.bytechef.component.helper.action.CryptoHelperSignAction;
import com.bytechef.component.helper.action.CryptoHelperVerifyAction;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class CryptoHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("cryptoHelper")
        .title("Crypto Helper")
        .description("The Crypto Helper allows you to use cryptographic functions.")
        .icon("path:assets/crypto-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            CryptoHelperHashAction.ACTION_DEFINITION,
            CryptoHelperHmacAction.ACTION_DEFINITION,
            CryptoHelperPgpDecryptAction.ACTION_DEFINITION,
            CryptoHelperPgpEncryptAction.ACTION_DEFINITION,
            CryptoHelperRsaDecryptAction.ACTION_DEFINITION,
            CryptoHelperRsaEncryptAction.ACTION_DEFINITION,
            CryptoHelperSignAction.ACTION_DEFINITION,
            CryptoHelperVerifyAction.ACTION_DEFINITION)
        .clusterElements(
            tool(CryptoHelperHashAction.ACTION_DEFINITION),
            tool(CryptoHelperHmacAction.ACTION_DEFINITION),
            tool(CryptoHelperPgpDecryptAction.ACTION_DEFINITION),
            tool(CryptoHelperPgpEncryptAction.ACTION_DEFINITION),
            tool(CryptoHelperRsaDecryptAction.ACTION_DEFINITION),
            tool(CryptoHelperRsaEncryptAction.ACTION_DEFINITION),
            tool(CryptoHelperSignAction.ACTION_DEFINITION),
            tool(CryptoHelperVerifyAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
