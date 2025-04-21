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

package com.bytechef.component.ai.vectorstore.typesense.constant;

import com.bytechef.component.ai.vectorstore.VectorStore;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.vectorstore.typesense.TypesenseVectorStore;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.resources.Node;

/**
 * @author Monika Kušter
 */
public class TypesenseConstants {

    public static final String API_KEY = "apiKey";
    public static final String COLLECTION = "collection";
    public static final String EMBEDDING_DIMENSION = "embeddingDimension";
    public static final String HOST = "host";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String PORT = "port";
    public static final String PROTOCOL = "protocol";
    public static final String TYPESENSE = "typesense";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        List<Node> nodes = List.of(
            new Node(
                connectionParameters.getRequiredString(PROTOCOL),
                connectionParameters.getRequiredString(HOST),
                connectionParameters.getRequiredString(PORT)));

        Configuration configuration = new Configuration(
            nodes, Duration.ofSeconds(5), connectionParameters.getRequiredString(API_KEY));

        return TypesenseVectorStore.builder(new Client(configuration), embeddingModel)
            .collectionName(connectionParameters.getRequiredString(COLLECTION))
            .embeddingDimension(connectionParameters.getRequiredInteger(EMBEDDING_DIMENSION))
            .initializeSchema(connectionParameters.getRequiredBoolean(INITIALIZE_SCHEMA))
            .build();
    };

    private TypesenseConstants() {
    }
}
