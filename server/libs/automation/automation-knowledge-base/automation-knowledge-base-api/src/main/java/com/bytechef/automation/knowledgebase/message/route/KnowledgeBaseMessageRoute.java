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

package com.bytechef.automation.knowledgebase.message.route;

import com.bytechef.message.route.MessageRoute;

/**
 * @author Ivica Cardic
 */
public enum KnowledgeBaseMessageRoute implements MessageRoute {

    DOCUMENT_PROCESS_EVENTS(MessageRoute.Exchange.MESSAGE, "knowledgeBase.document_process_events"),
    DOCUMENT_CHUNK_UPDATE_EVENTS(MessageRoute.Exchange.MESSAGE, "knowledgeBase.document_chunk_update_events");

    private MessageRoute.Exchange exchange;
    private String routeName;

    KnowledgeBaseMessageRoute() {
    }

    KnowledgeBaseMessageRoute(MessageRoute.Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    @Override
    public MessageRoute.Exchange getExchange() {
        return exchange;
    }

    @Override
    public String getName() {
        return routeName;
    }

    @Override
    public String toString() {
        return "KnowledgeBaseMessageRoute{" +
            "exchange=" + exchange +
            ", routeName='" + routeName + '\'' +
            "} ";
    }
}
