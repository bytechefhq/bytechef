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

package com.bytechef.automation.knowledgebase.event;

import com.bytechef.automation.knowledgebase.message.route.KnowledgeBaseMessageRoute;
import com.bytechef.message.event.MessageEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Event triggered when a knowledge base document chunk is updated.
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseDocumentChunkEvent implements MessageEvent<KnowledgeBaseMessageRoute> {

    protected Instant createDate;
    protected Long chunkId;
    protected String content;
    protected Map<String, Object> metadata = new HashMap<>();
    protected KnowledgeBaseMessageRoute route;

    public KnowledgeBaseDocumentChunkEvent() {
    }

    public KnowledgeBaseDocumentChunkEvent(Long chunkId, String content) {
        this.createDate = Instant.now();
        this.chunkId = chunkId;
        this.content = content;
        this.route = KnowledgeBaseMessageRoute.DOCUMENT_CHUNK_UPDATE_EVENTS;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public Object getMetadata(String name) {
        return metadata.get(name);
    }

    @Override
    public KnowledgeBaseMessageRoute getRoute() {
        return route;
    }

    @Override
    public void putMetadata(String name, Object value) {
        metadata.put(name, value);
    }
}
