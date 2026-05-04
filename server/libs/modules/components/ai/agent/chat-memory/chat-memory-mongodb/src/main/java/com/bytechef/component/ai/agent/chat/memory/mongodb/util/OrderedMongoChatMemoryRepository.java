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

package com.bytechef.component.ai.agent.chat.memory.mongodb.util;

import java.util.List;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

/**
 * Wraps a {@link ChatMemoryRepository} and replaces {@link #findConversationIds()} with a MongoDB aggregation pipeline
 * that groups by conversation ID and orders by the maximum message timestamp (DESC).
 *
 * @author ByteChef
 */
class OrderedMongoChatMemoryRepository implements ChatMemoryRepository {

    private static final String COLLECTION_NAME = "ai_chat_memory";

    private final ChatMemoryRepository delegate;
    private final MongoTemplate mongoTemplate;

    OrderedMongoChatMemoryRepository(ChatMemoryRepository delegate, MongoTemplate mongoTemplate) {
        this.delegate = delegate;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    @SuppressWarnings("NullAway")
    public List<String> findConversationIds() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("conversationId")
                .max("timestamp")
                .as("latestTimestamp"),
            Aggregation.sort(Sort.Direction.DESC, "latestTimestamp"));

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

        return results.getMappedResults()
            .stream()
            .map(doc -> doc.getString("_id"))
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return delegate.findByConversationId(conversationId);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        delegate.saveAll(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        delegate.deleteByConversationId(conversationId);
    }
}
