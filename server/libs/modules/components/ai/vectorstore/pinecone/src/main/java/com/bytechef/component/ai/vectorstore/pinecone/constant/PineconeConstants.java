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

package com.bytechef.component.ai.vectorstore.pinecone.constant;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;

/**
 * @author Monika Kušter
 */
public class PineconeConstants {

    public static final String API_KEY = "apiKey";
    public static final String HOST = "host";
    public static final String PINECONE = "pinecone";

    private static final Cache<String, org.springframework.ai.vectorstore.VectorStore> VECTOR_STORES =
        Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> VECTOR_STORES.get(
        connectionParameters.toString(), key -> {
            Pattern pattern = Pattern.compile("https:\\/\\/(.*)-(.*)\\.svc\\.(.*)\\.pinecone\\.io");
            Matcher matcher = pattern.matcher(connectionParameters.getRequiredString(HOST));

            if (matcher.find()) {
                return PineconeVectorStore.builder(embeddingModel)
                    .apiKey(connectionParameters.getRequiredString(API_KEY))
//                    .projectId(matcher.group(2))
//                    .environment(matcher.group(3))
                    .indexName(matcher.group(1))
                    .build();
            } else {
                throw new IllegalArgumentException("Invalid Host url");
            }
        });

    private PineconeConstants() {
    }
}
