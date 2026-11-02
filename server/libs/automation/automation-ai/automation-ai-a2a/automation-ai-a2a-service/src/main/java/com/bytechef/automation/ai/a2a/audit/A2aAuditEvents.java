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

package com.bytechef.automation.ai.a2a.audit;

/**
 * @author Ivica Cardic
 */
public final class A2aAuditEvents {

    public static final String A2A_PROJECT_CREATED = "A2A_PROJECT_CREATED";
    public static final String A2A_PROJECT_DELETED = "A2A_PROJECT_DELETED";
    public static final String A2A_PROJECT_UPDATED = "A2A_PROJECT_UPDATED";
    public static final String A2A_SERVER_CREATED = "A2A_SERVER_CREATED";
    public static final String A2A_SERVER_DELETED = "A2A_SERVER_DELETED";
    public static final String A2A_SERVER_UPDATED = "A2A_SERVER_UPDATED";

    private A2aAuditEvents() {
    }
}
