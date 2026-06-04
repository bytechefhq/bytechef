/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill;

/**
 * Shared constants for skill zip archive safety limits. Used by both the facade (upload/read) and the agent utils
 * (runtime extraction) to ensure consistent protection against zip bombs and oversized entries.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SkillArchiveConstants {

    public static final int MAX_ZIP_ENTRIES = 10_000;

    public static final int MAX_ZIP_ENTRY_SIZE = 10 * 1024 * 1024;

    private SkillArchiveConstants() {
    }
}
