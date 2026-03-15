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

package com.bytechef.ai.agent.skill;

/**
 * Shared constants for skill zip archive safety limits. Used by both the facade (upload/read) and the agent utils
 * (runtime extraction) to ensure consistent protection against zip bombs and oversized entries.
 *
 * @author Ivica Cardic
 */
public final class SkillArchiveConstants {

    public static final int MAX_ZIP_ENTRIES = 10_000;

    public static final int MAX_ZIP_ENTRY_SIZE = 10 * 1024 * 1024;

    private SkillArchiveConstants() {
    }
}
