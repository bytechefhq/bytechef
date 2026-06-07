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

package com.bytechef.component.mongodb.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.CONNECTION_STRING;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DATABASE;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

public class MongoDBConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(CONNECTION_STRING)
                        .label("Connection String")
                        .description(
                            "The MongoDB connection string. Supports both standard (mongodb://) and SRV " +
                                "(mongodb+srv://) formats, as well as TLS and authentication options.")
                        .defaultValue("mongodb://localhost:27017")
                        .exampleValue("mongodb+srv://cluster0.example.mongodb.net")
                        .required(true),
                    string(DATABASE)
                        .label("Database")
                        .description("The name of the database to connect to.")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("Username for authentication. Leave empty if credentials are in the " +
                            "connection string or no authentication is required.")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("Password for authentication. Leave empty if credentials are in the " +
                            "connection string or no authentication is required.")
                        .controlType(ControlType.PASSWORD)
                        .required(false)));

    private MongoDBConnection() {
    }
}
