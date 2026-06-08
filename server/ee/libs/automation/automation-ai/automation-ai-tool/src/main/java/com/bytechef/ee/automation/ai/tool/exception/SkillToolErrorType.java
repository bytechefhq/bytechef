/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
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

    private SkillToolErrorType(int errorKey) {
        super(SkillToolErrorType.class, errorKey);
    }
}
