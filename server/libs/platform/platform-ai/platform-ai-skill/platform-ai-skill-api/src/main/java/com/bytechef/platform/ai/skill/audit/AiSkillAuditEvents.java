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

package com.bytechef.platform.ai.skill.audit;

/**
 * @author Ivica Cardic
 */
public final class AiSkillAuditEvents {

    public static final String AI_SKILL_CONTENT_UPDATED = "AI_SKILL_CONTENT_UPDATED";
    public static final String AI_SKILL_CREATED = "AI_SKILL_CREATED";
    public static final String AI_SKILL_DELETED = "AI_SKILL_DELETED";
    public static final String AI_SKILL_FILES_ADDED = "AI_SKILL_FILES_ADDED";
    public static final String AI_SKILL_FILE_REMOVED = "AI_SKILL_FILE_REMOVED";
    public static final String AI_SKILL_UPDATED = "AI_SKILL_UPDATED";

    private AiSkillAuditEvents() {
    }
}
