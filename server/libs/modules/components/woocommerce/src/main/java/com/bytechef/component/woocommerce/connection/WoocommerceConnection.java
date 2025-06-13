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

package com.bytechef.component.woocommerce.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DOMAIN;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Marija Horvat
 */
public class WoocommerceConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(DOMAIN)
                        .label("Domain")
                        .description("The domain of your app.")
                        .required(true),
                    string(USERNAME)
                        .label("Consumer Key")
                        .description("The consumer key generated from your app.")
                        .required(true),
                    string(PASSWORD)
                        .label("Consumer Secret")
                        .description("The consumer secret generated from your app.")
                        .required(true)))
        .baseUri((connectionParameters, context) -> "https://"
            + connectionParameters.getRequiredString(DOMAIN) + "/wp-json/wc/v3");

    private WoocommerceConnection() {
    }
}
