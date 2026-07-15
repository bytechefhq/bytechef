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

package com.bytechef.automation.ai.tool.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Marko Kriskovic
 */
public class SkillToolErrorType extends AbstractErrorType {

    public static final SkillToolErrorType CREATE_SKILL = new SkillToolErrorType(100);
    public static final SkillToolErrorType DELETE_SKILL = new SkillToolErrorType(101);
    public static final SkillToolErrorType GET_SKILL = new SkillToolErrorType(102);
    public static final SkillToolErrorType GET_SKILL_FILE_CONTENT = new SkillToolErrorType(103);
    public static final SkillToolErrorType GET_SKILL_FILE_PATHS = new SkillToolErrorType(104);
    public static final SkillToolErrorType LIST_SKILLS = new SkillToolErrorType(105);
    public static final SkillToolErrorType UPDATE_SKILL = new SkillToolErrorType(106);
    public static final SkillToolErrorType UPDATE_SKILL_CONTENT = new SkillToolErrorType(107);
    public static final SkillToolErrorType REMOVE_SKILL_FILE = new SkillToolErrorType(108);

    private SkillToolErrorType(int errorKey) {
        super(SkillToolErrorType.class, errorKey);
    }
}
