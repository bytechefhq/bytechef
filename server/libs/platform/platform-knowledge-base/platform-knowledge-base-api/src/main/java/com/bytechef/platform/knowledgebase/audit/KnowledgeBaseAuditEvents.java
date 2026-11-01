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

package com.bytechef.platform.knowledgebase.audit;

/**
 * @author Ivica Cardic
 */
public final class KnowledgeBaseAuditEvents {

    public static final String KB_CLONED = "KB_CLONED";
    public static final String KB_CREATED = "KB_CREATED";
    public static final String KB_DELETED = "KB_DELETED";
    public static final String KB_DOCUMENT_CREATED = "KB_DOCUMENT_CREATED";
    public static final String KB_DOCUMENT_DELETED = "KB_DOCUMENT_DELETED";
    public static final String KB_UPDATED = "KB_UPDATED";

    private KnowledgeBaseAuditEvents() {
    }
}
