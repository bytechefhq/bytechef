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

package com.bytechef.platform.knowledgebase.exception;

/**
 * Thrown when a knowledge base document lookup fails because no row exists with the given id. Replaces the prior
 * pattern of throwing a plain {@code RuntimeException} whose message had to be string-matched downstream — that
 * approach was fragile against message renames, exception wrapping, and Spring data-access translation. Callers can now
 * {@code catch (KnowledgeBaseDocumentNotFoundException)} and treat the missing-document case as a benign no-op (e.g.
 * the deletion mutation applier's "already gone" path).
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseDocumentNotFoundException extends RuntimeException {

    private final long documentId;

    public KnowledgeBaseDocumentNotFoundException(long documentId) {
        super("KnowledgeBase document not found: " + documentId);

        this.documentId = documentId;
    }

    public long getDocumentId() {
        return documentId;
    }
}
