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

package com.bytechef.component.ai.chat.memory.constant;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryConstants {

    public static final String CONVERSATION_ID = "conversationId";
    public static final String MESSAGES = "messages";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_ROLE = "role";
    public static final String TOP_K = "topK";

    public static final String METADATA_CONVERSATION_ID = "conversationId";
    public static final String METADATA_MESSAGE_TYPE = "messageType";

    private VectorStoreChatMemoryConstants() {
    }
}
