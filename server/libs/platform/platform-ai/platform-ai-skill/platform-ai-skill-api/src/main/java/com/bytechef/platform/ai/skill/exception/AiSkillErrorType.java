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

package com.bytechef.platform.ai.skill.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.ai.skill.domain.AiSkill;

/**
 * @author Ivica Cardic
 */
public class AiSkillErrorType extends AbstractErrorType {

    public static final AiSkillErrorType CREATE_SKILL = new AiSkillErrorType(100);
    public static final AiSkillErrorType GET_SKILL = new AiSkillErrorType(102);
    public static final AiSkillErrorType GET_SKILL_FILE_CONTENT = new AiSkillErrorType(103);
    public static final AiSkillErrorType GET_SKILL_FILE_PATHS = new AiSkillErrorType(104);
    public static final AiSkillErrorType LIST_SKILLS = new AiSkillErrorType(105);
    public static final AiSkillErrorType UPDATE_SKILL = new AiSkillErrorType(106);
    public static final AiSkillErrorType UPDATE_SKILL_CONTENT = new AiSkillErrorType(107);
    public static final AiSkillErrorType REMOVE_SKILL_FILE = new AiSkillErrorType(108);

    private AiSkillErrorType(int errorKey) {
        super(AiSkill.class, errorKey);
    }
}
