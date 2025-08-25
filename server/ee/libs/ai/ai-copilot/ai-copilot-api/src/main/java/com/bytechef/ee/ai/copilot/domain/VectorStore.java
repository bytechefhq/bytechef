/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
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
