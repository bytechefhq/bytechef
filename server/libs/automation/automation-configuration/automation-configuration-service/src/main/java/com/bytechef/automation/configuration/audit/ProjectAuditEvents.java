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

package com.bytechef.automation.configuration.audit;

/**
 * @author Ivica Cardic
 */
public final class ProjectAuditEvents {

    public static final String PROJECT_CREATED = "PROJECT_CREATED";
    public static final String PROJECT_DELETED = "PROJECT_DELETED";
    public static final String PROJECT_DUPLICATED = "PROJECT_DUPLICATED";
    public static final String PROJECT_EXPORTED = "PROJECT_EXPORTED";
    public static final String PROJECT_IMPORTED = "PROJECT_IMPORTED";
    public static final String PROJECT_PUBLISHED = "PROJECT_PUBLISHED";
    public static final String PROJECT_SHARED_DELETED = "PROJECT_SHARED_DELETED";
    public static final String PROJECT_SHARED_EXPORTED = "PROJECT_SHARED_EXPORTED";
    public static final String PROJECT_TEMPLATE_IMPORTED = "PROJECT_TEMPLATE_IMPORTED";
    public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
    public static final String PROJECT_VERSION_DELETED = "PROJECT_VERSION_DELETED";
    public static final String PROJECT_VERSION_RESTORED = "PROJECT_VERSION_RESTORED";

    private ProjectAuditEvents() {
    }
}
