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

package com.bytechef.component.ai.llm.nano.gpt.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.BASE_URL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.DIMENSION;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.EMBEDDING_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.EmbeddingUtils;

/**
 * @author Marko Kriskovic
 */
public class NanoGptEmbedding {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<EmbeddingFunction>clusterElement("embedding")
            .title("NanoGPT Embedding")
            .description("NanoGPT embedding.")
            .type(EmbeddingFunction.EMBEDDING)
            .object(() -> NanoGptEmbedding::apply)
            .properties(EMBEDDING_MODEL_PROPERTY,
                integer(DIMENSION)
                    .label("Dimension")
                    .description("The number of dimensions for the output embeddings")
                    .minValue(1)
                    .defaultValue(1536)
                    .required(false));

    protected static EmbeddingModel apply(Parameters inputParameters, Parameters connectionParameters) {

        return new FloatEncodingEmbeddingModel(
            OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .apiKey(connectionParameters.getRequiredString(TOKEN))
                .build(),
            MetadataMode.ALL,
            inputParameters.getRequiredString(MODEL),
            inputParameters.getInteger(DIMENSION));
    }

    private static class FloatEncodingEmbeddingModel extends AbstractEmbeddingModel {

        private final OpenAIClient client;
        private final MetadataMode metadataMode;
        private final String model;
        private final Integer dimensions;

        FloatEncodingEmbeddingModel(
                OpenAIClient client, MetadataMode metadataMode, String model, Integer dimensions) {

            this.client = client;
            this.metadataMode = metadataMode;
            this.model = model;
            this.dimensions = dimensions;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            EmbeddingCreateParams.Builder builder = EmbeddingCreateParams.builder()
                .model(model)
                .encodingFormat(EmbeddingCreateParams.EncodingFormat.FLOAT)
                .input(EmbeddingCreateParams.Input.ofArrayOfStrings(request.getInstructions()));

            if (dimensions != null) {
                builder.dimensions(dimensions);
            }

            CreateEmbeddingResponse response = client.embeddings().create(builder.build());
            List<Embedding> embeddings = new ArrayList<>();

            for (com.openai.models.embeddings.Embedding embedding : response.data()) {
                embeddings.add(new Embedding(
                    EmbeddingUtils.toPrimitive(embedding.embedding()),
                    (int) embedding.index()));
            }

            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            EmbeddingResponse response = call(
                new EmbeddingRequest(List.of(document.getFormattedContent(metadataMode)), null));

            if (response.getResults().isEmpty()) {
                return new float[0];
            }

            return response.getResults().getFirst().getOutput();
        }
    }
}
