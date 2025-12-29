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

package com.bytechef.component.ai.vectorstore.transformer.splitter.cluster;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.platform.component.definition.ai.vectorstore.DocumentTransformerFunction.DOCUMENT_TRANSFORMER;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.DocumentSplitterFunction;
import java.util.List;
import org.springframework.ai.document.DocumentTransformer;

/**
 * @author Monika Kušter
 */
public class TokenTextSplitter {

    private static final String TOKEN_TEXT_SPLITTER = "tokenTextSplitter";

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<DocumentSplitterFunction>clusterElement("tokenTextSplitter")
            .title("Token Text Splitter")
            .description("Splitter that splits text into chunks of a target size in tokens.")
            .properties(
                object(TOKEN_TEXT_SPLITTER)
                    .label("Token Text Splitter")
                    .description("Splits text into chunks based on token count, using the CL100K_BASE encoding.")
                    .properties(
                        integer("defaultChunkSize")
                            .label("Default Chunk Size")
                            .description("The target size of each text chunk in tokens.")
                            .defaultValue(800)
                            .required(true),
                        integer("minChunkSizeChars")
                            .label("Minimum Chunk Size Characters")
                            .description("The minimum size of each text chunk in characters.")
                            .defaultValue(350)
                            .required(true),
                        integer("minChunkLengthToEmbed")
                            .label("Minimum Chunk Length to Embed")
                            .description("The minimum length of a chunk to be included.")
                            .defaultValue(5)
                            .required(true),
                        integer("maxNumChunks")
                            .label("Maximum Number of Chunks")
                            .description("The maximum number of chunks to generate from a text.")
                            .defaultValue(10000)
                            .required(true),
                        bool("keepSeparator")
                            .label("Keep Separator")
                            .description("Whether to keep separators (like newlines) in the chunks.")
                            .defaultValue(true)
                            .required(true))
                    .required(true))
            .type(DOCUMENT_TRANSFORMER)
            .object(() -> TokenTextSplitter::apply);

    protected static DocumentTransformer apply(
        Parameters inputParameters, Parameters connectionParameters) {

        TextSplitter textSplitter = inputParameters.get(TOKEN_TEXT_SPLITTER, TextSplitter.class);

        return new org.springframework.ai.transformer.splitter.TokenTextSplitter(
            textSplitter.defaultChunkSize(), textSplitter.minChunkSizeChars(), textSplitter.minChunkLengthToEmbed(),
            textSplitter.maxNumChunks(), textSplitter.keepSeparator(), List.of('.', '?', '!', '\n'));
    }

    record TextSplitter(
        int defaultChunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
        boolean keepSeparator) {
    }
}
