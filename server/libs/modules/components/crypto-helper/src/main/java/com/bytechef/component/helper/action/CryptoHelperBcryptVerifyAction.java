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

package com.bytechef.component.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.HASH;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PASSWORD;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

/**
 * @author Ivica Cardic
 */
public class CryptoHelperBcryptVerifyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("bcryptVerify")
        .title("BCrypt Verify")
        .description(
            "Checks a password against a BCrypt hash ($2a$, $2b$ or $2y$). Returns false for a hash that cannot be " +
                "read.")
        .properties(
            string(PASSWORD)
                .label("Password")
                .description("The password to check.")
                .required(true),
            string(HASH)
                .label("Hash")
                .description("The stored BCrypt hash, e.g. $2a$10$...")
                .required(true))
        .output(outputSchema(bool().description("Whether the password matches the hash")))
        .help("", "https://docs.bytechef.io/reference/components/crypto-helper_v1#bcrypt-verify")
        .perform(CryptoHelperBcryptVerifyAction::perform);

    private CryptoHelperBcryptVerifyAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String password = inputParameters.getString(PASSWORD);
        String hash = inputParameters.getString(HASH);

        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }

        try {
            return OpenBSDBCrypt.checkPassword(hash.trim(), password.toCharArray());
        } catch (DataLengthException | IllegalArgumentException exception) {
            return false;
        }
    }
}
