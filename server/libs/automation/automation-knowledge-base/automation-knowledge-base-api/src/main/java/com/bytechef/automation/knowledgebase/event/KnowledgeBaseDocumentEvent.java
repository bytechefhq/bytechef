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
 * @author Ivica Cardic
 */
public class KnowledgeBaseDocumentEvent implements MessageEvent<KnowledgeBaseMessageRoute> {

    protected Instant createDate;
    protected Long documentId;
    protected Map<String, Object> metadata = new HashMap<>();
    protected KnowledgeBaseMessageRoute route;

    public KnowledgeBaseDocumentEvent() {
    }

    public KnowledgeBaseDocumentEvent(Long documentId) {
        this.createDate = Instant.now();
        this.documentId = documentId;
        this.route = KnowledgeBaseMessageRoute.DOCUMENT_PROCESS_EVENTS;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    public Long getDocumentId() {
        return documentId;
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
