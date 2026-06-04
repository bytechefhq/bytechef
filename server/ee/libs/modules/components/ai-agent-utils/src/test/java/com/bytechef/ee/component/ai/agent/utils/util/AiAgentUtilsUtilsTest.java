/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.ai.agent.utils.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 * @version ee
 */
public class AiAgentUtilsUtilsTest {

    @Test
    void testIsScriptEntryTopLevel() {
        assertTrue(AiAgentUtilsUtils.isScriptEntry("scripts/main.js"));
    }

    @Test
    void testIsScriptEntryNested() {
        assertTrue(AiAgentUtilsUtils.isScriptEntry("skill-name/scripts/main.py"));
    }

    @Test
    void testIsScriptEntryReturnsFalseForMdFile() {
        assertFalse(AiAgentUtilsUtils.isScriptEntry("skill-name/SKILL.md"));
    }

    @Test
    void testIsScriptEntryReturnsFalseForReferences() {
        assertFalse(AiAgentUtilsUtils.isScriptEntry("skill-name/references/guide.md"));
    }

    @Test
    void testHasPerformFunctionJavaScript() {
        assertTrue(AiAgentUtilsUtils.hasPerformFunction("function perform(input, context) {\n  return null;\n}"));
    }

    @Test
    void testHasPerformFunctionPython() {
        assertTrue(AiAgentUtilsUtils.hasPerformFunction("def perform(input, context):\n  return None"));
    }

    @Test
    void testHasPerformFunctionR() {
        assertTrue(
            AiAgentUtilsUtils.hasPerformFunction("perform <- function(input, context) {\n  return(NULL)\n}"));
    }

    @Test
    void testHasPerformFunctionReturnsFalseWhenAbsent() {
        assertFalse(AiAgentUtilsUtils.hasPerformFunction("function helper() {}"));
    }
}
