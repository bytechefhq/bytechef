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

package com.bytechef.component.encryption.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.encryption.helper.action.EncryptionHelperDecryptAction;
import com.bytechef.component.encryption.helper.action.EncryptionHelperEncryptAction;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class EncryptionHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("encryptionHelper")
        .title("Encryption Helper")
        .description(
            "A secure PGP encryption and decryption service powered by Bouncy Castle," +
                " enabling users to encrypt, decrypt, and manage sensitive data with OpenPGP standards.")
        .icon("path:assets/encryption-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            EncryptionHelperDecryptAction.ACTION_DEFINITION,
            EncryptionHelperEncryptAction.ACTION_DEFINITION)
        .clusterElements(
            tool(EncryptionHelperDecryptAction.ACTION_DEFINITION),
            tool(EncryptionHelperEncryptAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
