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

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

/**
 * Delegates to a database-specific {@link JdbcChatMemoryRepositoryDialect} and points its SQL at a custom table.
 *
 * @author Marko Krišković
 */
class TableAwareChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {

    private final JdbcChatMemoryRepositoryDialect delegate;
    private final JdbcChatMemoryTable chatMemoryTable;

    TableAwareChatMemoryRepositoryDialect(
        JdbcChatMemoryRepositoryDialect delegate, JdbcChatMemoryTable chatMemoryTable) {

        this.delegate = delegate;
        this.chatMemoryTable = chatMemoryTable;
    }

    @Override
    public String getSelectMessagesSql() {
        return chatMemoryTable.rewriteSql(delegate.getSelectMessagesSql());
    }

    @Override
    public String getInsertMessageSql() {
        return chatMemoryTable.rewriteSql(delegate.getInsertMessageSql());
    }

    @Override
    public String getSelectConversationIdsSql() {
        return chatMemoryTable.rewriteSql(delegate.getSelectConversationIdsSql());
    }

    @Override
    public String getDeleteMessagesSql() {
        return chatMemoryTable.rewriteSql(delegate.getDeleteMessagesSql());
    }
}
