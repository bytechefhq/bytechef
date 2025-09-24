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

package com.bytechef.component.ai.vectorstore.neo4j.connection;

import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.DATABASE_NAME;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.DISTANCE_TYPE;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.EMBEDDING_DIMENSION;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.EMBEDDING_PROPERTY;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.INDEX_NAME;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.LABEL;
import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.URI;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;

/**
 * @author Monika Kušter
 */
public class Neo4jConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(URI)
                        .label("URI")
                        .description("URI for connecting to the Neo4j instance.")
                        .defaultValue("neo4j://localhost:7687")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("Username for authentication with Neo4j.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("Password for authentication with Neo4j.")
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the required schema")
                        .defaultValue(false)
                        .required(false),
                    string(DATABASE_NAME)
                        .label("Database Name")
                        .description("The name of the Neo4j database to use.")
                        .defaultValue("neo4j")
                        .required(false),
                    string(INDEX_NAME)
                        .label("Index Name")
                        .description("The name of the index to store the vectors.")
                        .defaultValue("spring-ai-document-index")
                        .required(false),
                    integer(EMBEDDING_DIMENSION)
                        .label("Embedding Dimension")
                        .description("The number of dimensions in the vector.")
                        .defaultValue(1536)
                        .required(false),
                    string(DISTANCE_TYPE)
                        .label("Distance Type")
                        .description("The distance function to use.")
                        .options(
                            option("Cosine", Neo4jVectorStore.Neo4jDistanceType.COSINE.name(),
                                "Suitable for most use cases. Measures cosine similarity between vectors."),
                            option("Euclidean", Neo4jVectorStore.Neo4jDistanceType.EUCLIDEAN.name(),
                                " Euclidean distance between vectors. Lower values indicate higher similarity."))
                        .defaultValue(Neo4jVectorStore.Neo4jDistanceType.COSINE.name())
                        .required(false),
                    string(LABEL)
                        .label("Label")
                        .description("The label used for document nodes.")
                        .defaultValue("Document")
                        .required(false),
                    string(EMBEDDING_PROPERTY)
                        .label("Embedding Property")
                        .description("The property name used to store embeddings.")
                        .defaultValue("embedding")
                        .required(false)));

    private Neo4jConnection() {
    }
}
