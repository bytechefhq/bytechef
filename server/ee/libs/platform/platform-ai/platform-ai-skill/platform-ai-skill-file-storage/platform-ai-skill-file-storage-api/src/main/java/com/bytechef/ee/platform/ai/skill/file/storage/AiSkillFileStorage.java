/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.file.storage;

import com.bytechef.file.storage.domain.FileEntry;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiSkillFileStorage {

    void deleteAiSkillFile(FileEntry skillFile);

    byte[] readAiSkillFileBytes(FileEntry skillFile);

    FileEntry storeAiSkillFile(String filename, byte[] bytes);
}
