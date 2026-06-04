/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.service;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiSkillService {

    AiSkill createAiSkill(AiSkill aiSkill);

    void deleteAiSkill(long id);

    boolean existsByName(String name);

    AiSkill getAiSkill(long id);

    List<AiSkill> getAiSkills();

    AiSkill updateAiSkill(long id, String name, @Nullable String description);

    AiSkill updateAiSkillFile(long id, FileEntry fileEntry);
}
