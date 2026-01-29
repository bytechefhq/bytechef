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

package com.bytechef.automation.knowledgebase.worker.etl;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.util.Assert;

/**
 * A document transformer that splits text into overlapping chunks based on token count. This provides better context
 * preservation when processing long texts for embeddings.
 *
 * <p>
 * This splitter uses the CL100K_BASE encoding (same as GPT-4) for tokenization. The chunk size is specified in tokens,
 * while minChunkSizeChars is in characters for finding good break points.
 * </p>
 *
 * @author Ivica Cardic
 */
public class OverlappingTokenTextSplitter implements DocumentTransformer {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 350;
    private static final int DEFAULT_OVERLAP = 100;
    private static final int DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int DEFAULT_MAX_NUM_CHUNKS = 10000;
    private static final List<Character> DEFAULT_PUNCTUATION_MARKS = List.of('.', '?', '!', '\n');

    private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
    private final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    private final int chunkSize;
    private final int minChunkSizeChars;
    private final int overlap;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final List<Character> punctuationMarks;

    public OverlappingTokenTextSplitter() {
        this(
            DEFAULT_CHUNK_SIZE, DEFAULT_MIN_CHUNK_SIZE_CHARS, DEFAULT_OVERLAP, DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED,
            DEFAULT_MAX_NUM_CHUNKS, DEFAULT_PUNCTUATION_MARKS);
    }

    /**
     * Creates an OverlappingTokenTextSplitter.
     *
     * @param chunkSize             the target size of each chunk in tokens
     * @param minChunkSizeChars     the minimum size of a chunk in characters for finding break points
     * @param overlap               the number of tokens to overlap between chunks
     * @param minChunkLengthToEmbed discard chunks shorter than this (in characters)
     * @param maxNumChunks          the maximum number of chunks to generate
     * @param punctuationMarks      punctuation marks to use for finding break points
     */
    public OverlappingTokenTextSplitter(
        int chunkSize, int minChunkSizeChars, int overlap, int minChunkLengthToEmbed, int maxNumChunks,
        List<Character> punctuationMarks) {

        Assert.isTrue(overlap < chunkSize, "Overlap must be less than chunk size");
        Assert.isTrue(overlap >= 0, "Overlap must be non-negative");
        Assert.notEmpty(punctuationMarks, "punctuationMarks must not be empty");

        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
        this.overlap = overlap;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks;
        this.punctuationMarks = List.copyOf(punctuationMarks);
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document document : documents) {
            result.addAll(splitDocument(document));
        }

        return result;
    }

    private List<Document> splitDocument(Document document) {
        List<Document> chunks = new ArrayList<>();
        String text = document.getText();

        if (text == null || text.trim()
            .isEmpty()) {
            return chunks;
        }

        List<Integer> tokens = getEncodedTokens(text);

        if (tokens.size() <= chunkSize) {
            if (text.trim()
                .length() >= minChunkLengthToEmbed) {
                chunks.add(copyDocumentWithText(document, text.trim()));
            }

            return chunks;
        }

        int numChunks = 0;
        int tokenStart = 0;

        while (tokenStart < tokens.size() && numChunks < maxNumChunks) {
            int tokenEnd = Math.min(tokenStart + chunkSize, tokens.size());
            List<Integer> chunkTokens = tokens.subList(tokenStart, tokenEnd);
            String chunkText = decodeTokens(chunkTokens);

            if (chunkText.trim()
                .isEmpty()) {
                tokenStart = tokenEnd;

                continue;
            }

            int actualTokensUsed = chunkTokens.size();

            if (tokens.size() > tokenEnd) {
                int lastPunctuation = getLastPunctuationIndex(chunkText);

                if (lastPunctuation != -1 && lastPunctuation > minChunkSizeChars) {
                    String truncatedText = chunkText.substring(0, lastPunctuation + 1);
                    actualTokensUsed = getEncodedTokens(truncatedText).size();
                    chunkText = truncatedText;
                }
            }

            String chunkTextTrimmed = chunkText.trim();

            if (chunkTextTrimmed.length() >= minChunkLengthToEmbed) {
                chunks.add(copyDocumentWithText(document, chunkTextTrimmed));
            }

            int advancement = actualTokensUsed - overlap;

            if (advancement <= 0) {
                advancement = Math.max(1, actualTokensUsed / 2);
            }

            tokenStart += advancement;
            numChunks++;
        }

        return chunks;
    }

    private int getLastPunctuationIndex(String chunkText) {
        int maxLastPunctuation = -1;

        for (Character punctuationMark : punctuationMarks) {
            int lastPunctuation = chunkText.lastIndexOf(punctuationMark);
            maxLastPunctuation = Math.max(maxLastPunctuation, lastPunctuation);
        }

        return maxLastPunctuation;
    }

    private List<Integer> getEncodedTokens(String text) {
        Assert.notNull(text, "Text must not be null");

        return encoding.encode(text)
            .boxed();
    }

    private String decodeTokens(List<Integer> tokens) {
        Assert.notNull(tokens, "Tokens must not be null");

        IntArrayList tokensIntArray = new IntArrayList(tokens.size());

        tokens.forEach(tokensIntArray::add);

        return encoding.decode(tokensIntArray);
    }

    private Document copyDocumentWithText(Document original, String newText) {
        Map<String, Object> metadata = original.getMetadata();

        return new Document(newText, metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int minChunkSizeChars = DEFAULT_MIN_CHUNK_SIZE_CHARS;
        private int overlap = DEFAULT_OVERLAP;
        private int minChunkLengthToEmbed = DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED;
        private int maxNumChunks = DEFAULT_MAX_NUM_CHUNKS;
        private List<Character> punctuationMarks = DEFAULT_PUNCTUATION_MARKS;

        public Builder withChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;

            return this;
        }

        public Builder withMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;

            return this;
        }

        public Builder withOverlap(int overlap) {
            this.overlap = overlap;

            return this;
        }

        public Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;

            return this;
        }

        public Builder withMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;

            return this;
        }

        public Builder withPunctuationMarks(List<Character> punctuationMarks) {
            this.punctuationMarks = List.copyOf(punctuationMarks);

            return this;
        }

        public OverlappingTokenTextSplitter build() {
            return new OverlappingTokenTextSplitter(
                chunkSize, minChunkSizeChars, overlap, minChunkLengthToEmbed, maxNumChunks, punctuationMarks);
        }
    }
}
