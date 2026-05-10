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

package com.bytechef.component.ai.agent.chat.memory.redis.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.SortedField;

/**
 * Wraps a {@link ChatMemoryRepository} and replaces {@link #findConversationIds()} with a Redis FT.AGGREGATE query that
 * groups by conversation ID and orders by the maximum message timestamp (DESC).
 *
 * @author ByteChef
 */
class OrderedRedisChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository delegate;
    private final JedisPooled jedisClient;
    private final String indexName;

    OrderedRedisChatMemoryRepository(ChatMemoryRepository delegate, JedisPooled jedisClient, String indexName) {
        this.delegate = delegate;
        this.jedisClient = jedisClient;
        this.indexName = indexName;
    }

    @Override
    public List<String> findConversationIds() {
        AggregationBuilder aggregation = new AggregationBuilder("*")
            .groupBy("@conversation_id", Reducers.max("@timestamp")
                .as("max_timestamp"))
            .sortBy(SortedField.desc("@max_timestamp"))
            .limit(0, 10000);

        AggregationResult result = jedisClient.ftAggregate(indexName, aggregation);

        List<String> conversationIds = new ArrayList<>();

        result.getResults()
            .forEach(row -> {
                String conversationId = (String) row.get("conversation_id");

                if (conversationId != null) {
                    conversationIds.add(conversationId);
                }
            });

        return conversationIds;
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
