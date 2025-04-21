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

package com.bytechef.platform.ai.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Marko Kriskovic
 */
@Table("vector_store")
@SuppressFBWarnings("EI")
public class VectorStore {

    @Id
    private UUID id;

    @Column
    private String content;

    @Column
    private Map<String, Object> metadata;

    @Column
    private List<Double> embedding;

    public VectorStore(UUID id, String content, Map<String, Object> metadata, List<Double> embedding) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
        this.embedding = embedding;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VectorStore that = (VectorStore) o;

        return Objects.equals(id, that.id) && Objects.equals(content, that.content)
            && Objects.equals(metadata, that.metadata) && Objects.equals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, metadata, embedding);
    }

    @Override
    public String toString() {
        return "VectorStore{" +
            "id=" + id +
            ", content='" + content + '\'' +
            ", metadata=" + metadata +
            ", embedding=" + embedding +
            '}';
    }
}
