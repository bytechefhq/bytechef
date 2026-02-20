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

package com.bytechef.component.ai.agent.chat.memory.builtin.repository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import redis.clients.jedis.JedisPooled;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis-based implementation of ChatMemoryRepository for storing chat messages.
 *
 * @author Ivica Cardic
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .build();

    private final JedisPooled jedisClient;
    private final String keyPrefix;
    private final Duration timeToLive;

    private RedisChatMemoryRepository(Builder builder) {
        this.jedisClient = builder.jedisClient;
        this.keyPrefix = builder.keyPrefix;
        this.timeToLive = builder.timeToLive;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = jedisClient.keys(keyPrefix + "*");
        List<String> conversationIds = new ArrayList<>();

        for (String key : keys) {
            String conversationId = key.substring(keyPrefix.length());

            conversationIds.add(conversationId);
        }

        return conversationIds;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = keyPrefix + conversationId;
        List<String> messagesJson = jedisClient.lrange(key, 0, -1);
        List<Message> messages = new ArrayList<>();

        for (String messageJson : messagesJson) {
            Message message = deserializeMessage(messageJson);

            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = keyPrefix + conversationId;

        jedisClient.del(key);

        for (Message message : messages) {
            String messageJson = serializeMessage(message);

            jedisClient.rpush(key, messageJson);
        }

        if (timeToLive != null) {
            jedisClient.expire(key, timeToLive.getSeconds());
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = keyPrefix + conversationId;

        jedisClient.del(key);
    }

    private String serializeMessage(Message message) {
        MessageData data = new MessageData(message.getMessageType()
            .getValue(), message.getText());

        return OBJECT_MAPPER.writeValueAsString(data);
    }

    private Message deserializeMessage(String json) {
        MessageData data = OBJECT_MAPPER.readValue(json, MessageData.class);

        return switch (data.type()) {
            case "USER" -> new UserMessage(data.content());
            case "ASSISTANT" -> new AssistantMessage(data.content());
            case "SYSTEM" -> new SystemMessage(data.content());
            default -> null;
        };
    }

    private record MessageData(String type, String content) {
    }

    @SuppressFBWarnings("EI")
    public static class Builder {

        private JedisPooled jedisClient;
        private String keyPrefix = "bytechef-chat-memory:";
        private Duration timeToLive;

        public Builder jedisClient(JedisPooled jedisClient) {
            this.jedisClient = jedisClient;

            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;

            return this;
        }

        public Builder timeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;

            return this;
        }

        public RedisChatMemoryRepository build() {
            if (jedisClient == null) {
                throw new IllegalArgumentException("JedisPooled client is required");
            }

            return new RedisChatMemoryRepository(this);
        }
    }
}
