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

package com.bytechef.component.ai.vectorstore.mongodbatlas.connection;

import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.COLLECTION_NAME;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.CONNECTION_STRING;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.DATABASE_NAME;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.INDEX_NAME;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.METADATA_FIELDS;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.NUM_CANDIDATES;
import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.PATH_NAME;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Alex Bevilacqua
 */
public class MongoDBAtlasConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(CONNECTION_STRING)
                        .label("Connection String")
                        .description(
                            "The MongoDB Atlas connection string, e.g. " +
                                "mongodb+srv://cluster0.example.mongodb.net.")
                        .exampleValue("mongodb+srv://cluster0.example.mongodb.net")
                        .required(true),
                    string(DATABASE_NAME)
                        .label("Database Name")
                        .description("The name of the database to store the vectors in.")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("Username for authentication with MongoDB Atlas.")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("Password for authentication with MongoDB Atlas.")
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(COLLECTION_NAME)
                        .label("Collection Name")
                        .description("The name of the collection to store the vectors in.")
                        .defaultValue("vector_store")
                        .required(false),
                    string(INDEX_NAME)
                        .label("Vector Index Name")
                        .description("The name of the Atlas Vector Search index.")
                        .defaultValue("vector_index")
                        .required(false),
                    string(PATH_NAME)
                        .label("Path Name")
                        .description("The path where the embeddings are stored within the document.")
                        .defaultValue("embedding")
                        .required(false),
                    integer(NUM_CANDIDATES)
                        .label("Number of Candidates")
                        .description("The number of candidates to consider during approximate nearest neighbor search.")
                        .defaultValue(200)
                        .required(false),
                    string(METADATA_FIELDS)
                        .label("Metadata Fields To Filter")
                        .description(
                            "A comma-separated list of metadata fields that can be used to filter search results.")
                        .required(false),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the collection and the vector search index.")
                        .defaultValue(false)
                        .required(false)));

    private MongoDBAtlasConnection() {
    }
}
